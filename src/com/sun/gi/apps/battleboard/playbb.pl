#!/usr/bin/perl -w
#
# $Id: playbb.pl,v 1.1 2006/03/06 22:04:15 de153050 Exp de153050 $

use FileHandle;
use IPC::Open2;

# NOTE:  if the BoardWidth and BoardHeight are not at least as large
# as the actual board, then there is no guarantee that the game will
# terminate.  It is not a problem if they are too large; the client
# (and server) will reject illegal moves.

$BoardWidth	= 4;
$BoardHeight	= 4;

$Props	= '-Dbattleboard.interactive=false';
$Class	= 'com.sun.gi.apps.battleboard.client.BattleBoardClient';

if (@ARGV < 3) {
    $x = @ARGV;
    print "$x usage: $0 UserName UserPasswd PlayerName\n";
    exit(1);
}

$UserName   = shift @ARGV;
$UserPasswd = shift @ARGV;
$PlayerName = shift @ARGV;

$Command	= "java -cp bin $Props $Class";

$GamesPlayed = 0;
$GamesWon    = 0;
$GamesLost   = 0;
$GamesError  = 0;

for (;;) {
    $rc = play($Command, $UserName, $UserPasswd, $PlayerName);
    $GamesPlayed += 1;
    if ($rc == 1) {
	$GamesWon += 1;
    } elsif ($rc == 0) {
	$GamesLost += 1;
    } else {
	$GamesError += 1;
	if ($rc < -1) {
	    die "** Game returned $rc -- exiting\n";
	}
    }
}

sub play {
    my ($cmd, $user, $passwd, $player, @opponents) = @_;

    my $pid = open2(*Reader, *Writer, $cmd);

    while (my $line = <Reader>) {
	if (!($line =~ /\|/)) {
	    print "== $line";
	}

	$line =~ s/\s+$//;

	if ($line eq "User Name:") {
	    print Writer "$user\n";
	    flush Writer;
	}
	elsif ($line eq "Password:") {
	    print Writer "$passwd\n";
	    flush Writer;
	}
	elsif ($line eq "Enter your handle [$user]:") {
	    print Writer "$player\n";
	    flush Writer;
	    last;
	}
    }

    print "Starting game...\n";
    flush STDOUT;

    for (my $x = 0; $x < $BoardWidth; $x++) {
	for (my $y = 0; $y < $BoardHeight; $y++) {

	    while (my $line = <Reader>) {
		if ($line =~ /^\s+$/) {
			next;
		}
		if (!($line =~ /\*/)) {
		    print "== $line";
		    flush STDOUT;
		}

		$line =~ s/\s+$//;

		if ($line eq "player x y, or pass") {
		    print Writer "$player $x $y\n";
		    flush Writer;
		    last;
		}
		elsif ($line eq "YOU WIN!") {
		    close Reader;
		    close Writer;
		    wait;
		    return 1;
		}
		elsif ($line =~ /WINS!$/) {
		    close Reader;
		    close Writer;
		    wait;
		    return 0;
		}
		elsif ($line =~ /is not in the game\.$/) {
		    kill "TERM", $pid;
		    close Reader;
		    close Writer;
		    wait;
		    return -1;
		}
		elsif ($line =~ /Connection refused/) {
		    close Reader;
		    close Writer;
		    wait;
		    return -2;
		}
	    }
	}
    }
}
