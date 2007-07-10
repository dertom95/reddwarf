#
#
OS = $(shell uname)
CC = gcc

ifeq ($(OS),SunOS)
  CFLAGS = -Wall -O -std=c99 -pedantic -D __EXTENSIONS__
  LINKFLAGS = -lnsl -lsocket
else
  CFLAGS = -Wall -O -std=c99 -pedantic
  LINKFLAGS =
endif

ODIR = obj
BINDIR = bin
SRCDIR = src/client/c

API_HEADERS = $(wildcard $(SRCDIR)/*.h)
SRCS = $(notdir $(wildcard $(SRCDIR)/*.c)) $(addprefix impl/, $(notdir $(wildcard $(SRCDIR)/impl/*.c)))
OBJS = $(addprefix $(ODIR)/, $(SRCS:.c=.o))

# automatic variable reminders:
#  $@ = rule target
#  $< = first prerequisite
#  $? = all prerequisites that are newer than the target
#  $^ = all prerequisites

.PHONY:	clean tar run

all:	chatclient tests

$(ODIR)/%.o: $(SRCDIR)/%.c $(API_HEADERS) $(SRCDIR)/%.h
	@mkdir -p $(ODIR)
	$(CC) $(CFLAGS) -I $(SRCDIR) -c $< -o $@

$(ODIR)/example/%.o: $(SRCDIR)/example/%.c $(API_HEADERS)
	@mkdir -p $(ODIR)/example
	$(CC) $(CFLAGS) -I $(SRCDIR) -c $< -o $@

$(ODIR)/impl/%.o: $(SRCDIR)/impl/%.c $(API_HEADERS) $(SRCDIR)/impl/%.h
	@mkdir -p $(ODIR)/impl
	$(CC) $(CFLAGS) -I $(SRCDIR) -c $< -o $@

$(ODIR)/test/%.o: $(SRCDIR)/test/%.c $(API_HEADERS)
	@mkdir -p $(ODIR)/test
	$(CC) $(CFLAGS) -I $(SRCDIR) -I $(SRCDIR)/impl -c $< -o $@

chatclient: $(OBJS) $(ODIR)/example/sgs_chat_client.o $(API_HEADERS)
	@mkdir -p $(BINDIR)
	$(CC) $(CFLAGS) $(LINKFLAGS) -o $(BINDIR)/chatclient $^

buffertest: $(ODIR)/impl/sgs_buffer_impl.o $(ODIR)/test/sgs_buffer_test.o
	@mkdir -p $(BINDIR)
	$(CC) $(CFLAGS) $(LINKFLAGS) -o $(BINDIR)/buffertest $^

idtest: $(ODIR)/impl/sgs_compact_id.o $(ODIR)/test/sgs_id_test.o $(ODIR)/sgs_hex_utils.o
	@mkdir -p $(BINDIR)
	$(CC) $(CFLAGS) $(LINKFLAGS) -o $(BINDIR)/idtest $^

maptest: $(ODIR)/impl/sgs_linked_list_map.o $(ODIR)/test/sgs_map_test.o
	@mkdir -p $(BINDIR)
	$(CC) $(CFLAGS) $(LINKFLAGS) -o $(BINDIR)/maptest $^

messagetest: $(ODIR)/sgs_message.o $(ODIR)/impl/sgs_compact_id.o $(ODIR)/sgs_hex_utils.o $(ODIR)/test/sgs_message_test.o
	@mkdir -p $(BINDIR)
	$(CC) $(CFLAGS) $(LINKFLAGS) -o $(BINDIR)/messagetest $^

tests: buffertest idtest maptest messagetest

tar:
	mv -f c_backups.tar c_backups.tar.prev
	tar cf c_backups.tar $(SRCDIR)

clean:
	rm -f *~ core callbacks.out $(SRCDIR)/*~ $(SRCDIR)/*/*~ $(ODIR)/*.o $(ODIR)/*/*.o $(SRCDIR)/*.gch $(SRCDIR)/*/*.gch
