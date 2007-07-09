/*
 * Copyright 2007 Sun Microsystems, Inc. All rights reserved
 */

/*
 * This file implements a client for the example-chat-app SGS application.
 *
 * Conventions:
 *  Unless otherwise noted, all functions with an int return type return
 *  0 upon success and -1 upon failure, while also setting errno to the
 *  specific error code.
 */

#include <errno.h>
#include <poll.h>
#include <stdio.h>
#include <string.h>
/* required for STDIN_FILENO on Linux/Solaris, but not Darwin: */
#include <unistd.h>

/** included for optarg (declared in unistd.h on some, but not all systems) */
#include <getopt.h>
#include "sgs_connection.h"
#include "sgs_context.h"
#include "sgs_map.h"
#include "sgs_session.h"

/** Timeout value for calls to poll() */
#define POLL_TIMEOUT  200

/** Default connection info for server. */
#define DEFAULT_HOST  "localhost"
#define DEFAULT_PORT  2502

/** The name of the global channel */
#define GLOBAL_CHANNEL_NAME  "-GLOBAL-"

/*
 * Message callbacks
 */
static void channel_joined_cb(sgs_connection *conn, sgs_channel *channel);
static void channel_left_cb(sgs_connection *conn, sgs_channel *channel);
static void channel_recv_msg_cb(sgs_connection *conn, sgs_channel *channel,
    const sgs_id *sender_id, const uint8_t *msg,
    size_t msglen);
static void disconnected_cb(sgs_connection *conn);
static void logged_in_cb(sgs_connection *conn, sgs_session *session);
static void login_failed_cb(sgs_connection *conn, const uint8_t *msg,
    size_t msglen);
static void reconnected_cb(sgs_connection *conn);
static void recv_msg_cb(sgs_connection *conn, const uint8_t *msg, size_t msglen);
static void register_fd_cb(sgs_connection *conn, int fd, short events);
static void unregister_fd_cb(sgs_connection *conn, int fd, short events);

/*
 * STATIC FUNCTION DECLARATIONS
 * (can only be called by functions in this file)
 */
static void bye(int exitval);
static int concatstr(const char *prefix, const char *suffix, char *buf,
    size_t buflen);
static int fprint_fixed_len_str(FILE *stream, const uint8_t *data, size_t len);
static void process_user_cmd(char *cmd);

/*
 * STATIC GLOBAL VARIABLES
 *
 * Some of these are declared globally so that the various callback functions
 * can access them; others are declared globally just so that they will persist
 * between function calls without having to be redeclared and initialized each
 * time.  Another way to do this would be to declare a globally-accessible
 * lookup service which, given a sgs_connection as the key, allows access to
 * independent sets of these variables for each connection.  This would support
 * multiple, concurrent connections/sessions.
 */
static sgs_context *g_context;
static sgs_connection *g_conn;
static sgs_session *g_session;
static sgs_map *g_channel_map;
static struct pollfd g_poll_fds[50];
static int g_nfds;

/*
 * function: main()
 */
int main(int argc, char *argv[]) {
    char inbuf[1024] = { '\0' };   /** inbuf must always be null-terminated */
    int inbuf_alive = 0;
    char *token;
    int c, i, len, result, token_len, work;
    char *hostname = DEFAULT_HOST;
    int port = DEFAULT_PORT;
    
    /**
     * stdout and stderr are normally line-buffered, but if they are redirected
     * to a file (instead of the console) this may not be true; this can be
     * annoying so force them both to be line-buffered no matter what.
     */
    setvbuf(stdout, (char *)NULL, _IOLBF, 0);
    setvbuf(stderr, (char *)NULL, _IOLBF, 0);
    
    /** We are always interested in reading from STDIN. */
    g_poll_fds[0].fd = STDIN_FILENO;
    g_poll_fds[0].events = POLLIN;
    g_nfds = 1;
    
    /** process command line arguments */
    while ((c = getopt(argc, argv, "h:p:u")) != -1) {
        switch (c) {
        case 'h':  /* hostname */
            hostname = optarg;
            break;
      
        case 'p':  /* port */
            port = atoi(optarg);
            break;
      
        case 'u':  /* usage */
            printf("Usage: %s [-h HOST] [-p PORT] [-u]\n  -h    Specify remote \
hostname (default: %s)\n  -p    Specify remote port (default: %d)\n  -u    Prin\
t usage\n",
                argv[0], DEFAULT_HOST, DEFAULT_PORT);
            return 0;
      
            /* 
             * No default case necessary; an error will automatically be printed
             * since opterr is 1.
             */
        }
    }
    
    printf("Starting up with host=%s and port=%d...\n", hostname, port);
    
    /**
     * Create map to hold channel pointers.  Use strcmp to compare map keys
     * since the keys are channel names.  Do not pass functions to free map
     * keys or values since we are not allocating these (just copying pointers
     * passed by callback functions).
     */
    g_channel_map = sgs_map_new((int (*)(const void*,const void*))strcmp, NULL,
        NULL);
    
    /** Create sgs_context object and register event callbacks. */
    g_context = sgs_ctx_new(hostname, port, register_fd_cb, unregister_fd_cb);
    if (g_context == NULL) { perror("Error in sgs_ctx_new()"); bye(-1); }
    
    sgs_ctx_set_channel_joined_cb(g_context, channel_joined_cb);
    sgs_ctx_set_channel_left_cb(g_context, channel_left_cb);
    sgs_ctx_set_channel_recv_msg_cb(g_context, channel_recv_msg_cb);
    sgs_ctx_set_disconnected_cb(g_context, disconnected_cb);
    sgs_ctx_set_logged_in_cb(g_context, logged_in_cb);
    sgs_ctx_set_login_failed_cb(g_context, login_failed_cb);
    sgs_ctx_set_reconnected_cb(g_context, reconnected_cb);
    sgs_ctx_set_recv_msg_cb(g_context, recv_msg_cb);
    
    /** Create sgs_connection object. */
    g_conn = sgs_connection_new(g_context);
    if (g_conn == NULL) { perror("Error in sgs_connection_new()"); bye(-1); }
    
    printf("Command: ");
    fflush(stdout);
    
    while (1) {
        if (inbuf_alive && strlen(inbuf) > 0) {
            len = strlen(inbuf);
            
            /**
             * Note: If strtok is called on a string with no characters from
             * set, strtok will return the string, not NULL, which is not the
             * behavior that we want in this case (if there is no terminating
             * newline character, then we have a partial line and we want to
             * wait for more input).  So we need a special check for this case.
             */
            token = strtok(inbuf, "\n");
            
            if (token == NULL || strlen(token) == len) {
                inbuf_alive = 0;
            } else {
                token_len = strlen(token);
                process_user_cmd(token);
                memmove(inbuf, token + token_len + 1, len - (token_len + 1) + 1);
            }
        }
        
        result = poll(g_poll_fds, g_nfds, POLL_TIMEOUT);
        
        if (result == -1) {
            perror("Error calling poll()");
        }
        else if (result > 0) {
            work = 0;
            
            for (i=0; i < g_nfds; i++) {
                if (g_poll_fds[i].revents != 0) {
                    /** STDIN */
                    if (g_poll_fds[i].fd == STDIN_FILENO) {
                        if ((g_poll_fds[i].revents & POLLIN) == POLLIN) {
                            /** Data available for reading. */
                            len = strlen(inbuf);
                            result = read(STDIN_FILENO, inbuf + len,
                                sizeof(inbuf) - strlen(inbuf) - 1);
              
                            if (result == -1) {
                                perror("Error calling read() on STDIN");
                            }
                            else if (result > 0) {
                                /**
                                 * Always null-terminate the block of data in
                                 * inbuf so that strtok can be called on it.
                                 */
                                inbuf[len + result] = '\0';
                                inbuf_alive = 1;
                
                                printf("Command: ");
                                fflush(stdout);
                            }
                        }
                    } else if (g_poll_fds[i].fd != -1) {
                        /** Must be some fd that the sgs_connection registered */
                        work = 1;
                    }
                }
            }
            
            if (work) {
                if (sgs_connection_do_work(g_conn)) {
                    perror("Error calling sgs_connection_do_work()");
                }
            }
        }
        /** else, poll() timed out. */
    }
    
    /** Just for compiler; should never reach here. */
    return 0;
}


/*
 * INTERNAL (STATIC) FUNCTION IMPLEMENTATIONS
 * (these are functions that can only be called within this file)
 */


/*
 * -----  Callback Functions -----
 */


/*
 * channel_joined_cb()
 */
static void channel_joined_cb(sgs_connection *conn, sgs_channel *channel) {
    const char *name = sgs_channel_get_name(channel);
    int result = sgs_map_put(g_channel_map, (char*)name, channel);
    printf(" - Callback -   Joined channel: %s\n", name);
    
    if (result == 0) {
        printf("Warning: client thought it was already a member of channel %s\
...\n", name);
    } else if (result == -1) {
        perror("Error in sgs_map_put()");
    }
}

/*
 * channel_left_cb()
 */
static void channel_left_cb(sgs_connection *conn, sgs_channel *channel) {
    const char *name = sgs_channel_get_name(channel);
    int result = sgs_map_remove(g_channel_map, name);
    printf(" - Callback -   Left channel: %s\n", name);
    
    if (result == -1) {
        printf("Warning: client did not think it was a member of channel %s\
...\n", name);
    }
}

/*
 * channel_recv_msg_cb()
 */
static void channel_recv_msg_cb(sgs_connection *conn, sgs_channel *channel,
    const sgs_id *sender_id, const uint8_t *msg, size_t msglen)
{
    const char *channel_name = sgs_channel_get_name(channel);
    const char *sender_desc = (sender_id == NULL) ? "Server" :
        sgs_id_printable(sender_id);
  
    printf(" - Callback -   Received message on channel %s from %s: ",
        channel_name, sender_desc);
  
    fprint_fixed_len_str(stdout, msg, msglen);
    printf("\n");
}

/*
 * disconnected_cb()
 */
static void disconnected_cb(sgs_connection *conn) {
    printf(" - Callback -   Disconnected.\n");
}

/*
 * logged_in_cb()
 */
static void logged_in_cb(sgs_connection *conn, sgs_session *session) {
    printf(" - Callback -   Logged in with sessionId %s.\n",
        sgs_id_printable(sgs_session_get_id(session)));
  
    g_session = session;
}

/*
 * login_failed_cb()
 */
static void login_failed_cb(sgs_connection *conn, const uint8_t *msg,
    size_t msglen)
{
    printf(" - Callback -   Login failed (");
    fprint_fixed_len_str(stdout, msg, msglen);
    printf(").\n");
}

/*
 * reconnected_cb()
 */
static void reconnected_cb(sgs_connection *conn) {
    printf(" - Callback -   Reconnected.\n");
}

/*
 * recv_msg_cb()
 */
static void recv_msg_cb(sgs_connection *conn, const uint8_t *msg,
    size_t msglen)
{
    printf(" - Callback -   Received message: ");
    fprint_fixed_len_str(stdout, msg, msglen);
    printf("\n");
}

/*
 * register_fd_cb()
 */
static void register_fd_cb(sgs_connection *conn, int fd, short events) {
    int i, found;
    
    found = 0;
    
    for (i=0; i < g_nfds; i++) {
        if (fd == g_poll_fds[i].fd) {
            found = 1;
            g_poll_fds[i].events |= events;  /** Turn on requested bits */
            break;
        }
    }
    
    if (!found) {
        /** Need a new entry in g_poll_fds[] for this file descriptor. */
        if (g_nfds == sizeof(g_poll_fds)) {
            fprintf(stderr, "Error: Too many file descriptors registered.  \
Ignoring requests.\n");
        } else {
            g_poll_fds[g_nfds].fd = fd;
            g_poll_fds[g_nfds].events = events;
            
            /**
             * We have to set revents to 0 for all new entries in case this
             * callback is called while in the middle of iterating
             * g_poll_fds[] after a poll().
             */
            g_poll_fds[g_nfds].revents = 0;
            g_nfds++;
        }
    }
}

/*
 * unregister_fd_cb()
 */
static void unregister_fd_cb(sgs_connection *conn, int fd, short events) {
    int i, last_max = 0, resize = 0;
    
    for (i=0; i < g_nfds; i++) {
        if (fd == g_poll_fds[i].fd) {
            if (events == 0) {
                /** Turn off all bits */
                g_poll_fds[i].events = 0;
            }
            else {
                /** Turn off requested bits */
                g_poll_fds[i].events &= ~events;
            }
            
            if (g_poll_fds[i].events == 0) {
                g_poll_fds[i].fd = -1;
                
                if (i == g_nfds-1) {
                    /** Last fd in array was cleared, so resize array */
                    last_max = i;
                    resize = 1;
                }
            }
            
            break;
        }
    }
    
    if (resize) {
        for (i=0; i <= last_max; i++) {
            if (g_poll_fds[i].fd != -1) {
                g_nfds = i + 1;
            }
        }
    }
}


/*
 * -----  Other Static Methods -----
 */


/*
 * function: bye()
 *
 * Performs object cleanup and then exits.
 */
static void bye(int exitval) {
    /* cleanup: */
    if (g_context != NULL) sgs_ctx_free(g_context);
    if (g_conn != NULL) sgs_connection_free(g_conn);
    if (g_channel_map != NULL) sgs_map_free(g_channel_map);
    
    exit(exitval);
}

/*
 * function: concatstr()
 *
 * Copies prefix and suffix to buf, after checking that buf has enough space.
 * Returns 0 on success or -1 on error.
 */
static int concatstr(const char *prefix, const char *suffix, char *buf,
    size_t buflen)
{
    if (strlen(prefix) + strlen(suffix) + 1 > buflen) {
        errno = ENOBUFS;
        return -1;
    }
  
    memcpy(buf, prefix, strlen(prefix));
    memcpy(buf + strlen(prefix), suffix, strlen(suffix) + 1);  /* include '\0' */
    return 0;
}

/*
 * function: fprint_fixed_len_str()
 *
 * Prints a fixed length string to the specified stream; since the length of the
 *  string is passed as an argument, the string does not have to end with a null
 *  ('\0') character (and null characters encountered before the datalen-th
 *  character of the string will NOT terminate the copying).
 */
static int fprint_fixed_len_str(FILE *stream, const uint8_t *data, size_t len) {
    int i;
  
    for (i=0; i < len; i++)
        fputc(data[i], stream);
  
    return len;
}

/*
 * function: process_user_cmd()
 *
 * Process and act on a line of user input.
 *
 * args:
 *   input: string of text entered by the user
 */
static void process_user_cmd(char *cmd) {
    char *token, *token2, *tmp;
    char strbuf[1024];
    sgs_id recipient;
    sgs_channel *channel;
    
    token = strtok(cmd, " ");
    
    if (token == NULL) {
        /** nothing entered? */
    }
    else if (strcmp(token, "help") == 0) {
        printf("Available commands:\n");
        printf("  quit: terminates the program\n");
        printf("  login <username> <password>: log into the server\n");
        printf("  logout: log out from the server (cleanly)\n");
        printf("  logoutf: log out from the server (forcibly)\n");
        printf("  srvsend <msg>: send a message directly to the server (not \
normally necessary)\n");
        printf("  psend <user-id> <msg>: send a private message to a user (alias\
: pm)\n");
        printf("  chsend <channel-name> <msg>: broadcast a message on a channel\n");
        printf("  chjoin <channel-name>: join a channel (alias: join)\n");
        printf("  chleave <channel-name>: leave a channel (alias: leave)\n");
        printf("\n");
    }
    else if ((strcmp(token, "quit") == 0) || (strcmp(token, "exit") == 0)) {
        bye(0);
    }
    else if (strcmp(token, "login") == 0) {
        token = strtok(NULL, " ");
    
        if (token == NULL) {
            printf("Invalid command.  Syntax: login <username> <password>\n");
            return;
        }
    
        token2 = strtok(NULL, " ");
    
        if (token2 == NULL) {
            printf("Invalid command.  Syntax: login <username> <password>\n");
            return;
        }
    
        tmp = strtok(NULL, "");
    
        if (tmp == token2) {
            printf("Invalid command.  Syntax: login <username> <password>\n");
            return;
        }
    
        if (sgs_connection_login(g_conn, token, token2) == -1) {
            perror("Error in sgs_connection_login()");
            return;
        }
    }
    else if (strcmp(token, "logout") == 0) {
        if (sgs_connection_logout(g_conn, 0) == -1) {
            perror("Error in sgs_connection_logout()");
            return;
        }
    }
    else if (strcmp(token, "logoutf") == 0) {
        if (sgs_connection_logout(g_conn, 1) == -1) {
            perror("Error in sgs_connection_logout()");
            return;
        }
    }
    else if (strcmp(token, "srvsend") == 0) {
        if (g_session == NULL) {
            printf("Error: not logged in!\n");
            return;
        }
    
        token = strtok(NULL, "");
    
        if (token == NULL) {
            printf("Invalid command.  Syntax: srvsend <msg>\n");
            return;
        }
    
        if (sgs_session_direct_send(g_session, (uint8_t*)token,
                strlen(token)) == -1) {
            perror("Error in sgs_session_direct_send()");
            return;
        }
    }
    else if (strcmp(token, "psend") == 0 || strcmp(token, "pm") == 0) {
        if (g_session == NULL) {
            printf("Error: not logged in!\n");
            return;
        }
        
        /** For private messages, use the "Global" channel */
        channel = sgs_map_get(g_channel_map, GLOBAL_CHANNEL_NAME);
        if (channel == NULL) {
            printf("Error: could not find global channel in channel map.");
            return;
        }
        
        token = strtok(NULL, " ");
        
        if (token == NULL) {
            printf("Invalid command.  Syntax: psend <user> <msg>\n");
            return;
        }
        
        if (sgs_id_init_from_hex(token, &recipient) == -1) {
            printf("Invalid user ID.\n");
            return;
        }
        
        token = strtok(NULL, "");
        
        if (token == NULL) {
            printf("Invalid command.  Syntax: psend <user> <msg>\n");
            return;
        }
        
        if (concatstr("/pm ", token, strbuf, sizeof(strbuf)) == -1) {
            printf("Error: ran out of buffer space (user input too big?).");
            return;
        }
        
        if (sgs_channel_send_one(channel, (uint8_t*)strbuf, strlen(strbuf),
                recipient) == -1) {
            perror("Error in sgs_session_channel_send()");
            return;
        }
    }
    else if (strcmp(token, "chsend") == 0) {
        if (g_session == NULL) {
            printf("Error: not logged in!\n");
            return;
        }
        
        token = strtok(NULL, " ");
        
        if (token == NULL) {
            printf("Invalid command.  Syntax: chsend <channel> <msg>\n");
            return;
        }
        
        channel = sgs_map_get(g_channel_map, token);
        if (channel == NULL) {
            printf("Error: Channel \"%s\" not found.\n", token);
            return;
        }
        
        token = strtok(NULL, "");
        
        if (token == NULL) {
            printf("Invalid command.  Syntax: chsend <channel> <msg>\n");
            return;
        }
        
        /** note: no prefix necessary for this command */
        
        if (sgs_channel_send_all(channel, (uint8_t*)token, strlen(token)) == -1) {
            perror("Error in sgs_session_channel_send()");
            return;
        }
    }
    else if (strcmp(token, "chjoin") == 0 || strcmp(token, "join") == 0) {
        if (g_session == NULL) {
            printf("Error: not logged in!\n");
            return;
        }
        
        token = strtok(NULL, "");
        
        if (token == NULL) {
            printf("Invalid command.  Syntax: chjoin <channel-name>\n");
            return;
        }
        
        if (concatstr("/join ", token, strbuf, sizeof(strbuf)) == -1) {
            printf("Error: ran out of buffer space (user input too big?).");
            return;
        }
        
        if (sgs_session_direct_send(g_session, (uint8_t*)strbuf,
                strlen(strbuf)) == -1) {
            perror("Error in sgs_session_direct_send()");
            return;
        }
    }
    else if (strcmp(token, "chleave") == 0 || strcmp(token, "leave") == 0) {
        if (g_session == NULL) {
            printf("Error: not logged in!\n");
            return;
        }
        
        token = strtok(NULL, "");
        
        if (token == NULL) {
            printf("Invalid command.  Syntax: chleave <channel-name>\n");
            return;
        }
        
        if (concatstr("/leave ", token, strbuf, sizeof(strbuf)) == -1) {
            printf("Error: ran out of buffer space (user input too big?).");
            return;
        }
        
        if (sgs_session_direct_send(g_session, (uint8_t*)strbuf,
                strlen(strbuf)) == -1) {
            perror("Error in sgs_session_direct_send()");
            return;
        }
    }
    else {
        printf("Unrecognized command.  Try \"help\"\n");
    }
}
