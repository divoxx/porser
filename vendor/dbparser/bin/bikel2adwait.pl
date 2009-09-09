#!/usr/bin/perl -w

# converts our tagged format to Adwait's which is one sentence per line,
# each sentence having the form
#  word1_tag1 word2_tag2

while (<>) {
    s/[()]//g;
    @items = split;
    if (@items % 2 != 0) {
	print STDERR "warning: unequal number of items in line";
	print STDERR $_;
    }
    for ($i = 0; $i < @items; $i += 2) {
	print $items[$i], "_", $items[($i + 1)], " ";
    }
    print "\n";
}
