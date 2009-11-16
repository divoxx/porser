#!/usr/bin/perl -w

##PRE-TOkENIZER PARA O GALEGO ILG-RAG

use strict;
use locale;
use POSIX;

setlocale(LC_CTYPE,"pt_PT");


my $vogalacentuada =  "\[·ÈÌÛ˙¡…Õ”⁄\]";

my $token;
my $token1;
my $token2;
my $root;
my $tail;
while ($token = <>) {

    chomp $token;
    
    if  ($token =~ /-lo(s?)$|-la(s?)$/i) {

       if ($token =~ /$vogalacentuada/) {
           $token =~  y/·ÈÌÛ˙¡…Õ”⁄/aeiouAEIOU/;
	   ($root, $tail) = split ("-", $token);
           $token1 = $root . "s" ;
           ($token2) = ($tail =~ /l([\w]+)/);
           print "$token1\n" ;
           print "$token2\n" ;
       }
       elsif ($token !~ /$vogalacentuada/) {
           
	   ($root, $tail) = split ("-", $token);
           $token1 = $root . "r" ;
           ($token2) = ($tail =~ /l([\w]+)/);
           print "$token1\n" ;
           print "$token2\n" ;
       }
      
   }
   elsif ($token =~ /tÛd[ao]l[ao](s?)$/i) {
       ($root, $tail) = ($token =~ /(tÛd[oa])l([ao](s?))$/i) ;
        $root =~ y/Û”/oO/;
        print $root . "s\n" ;
        print "$tail\n" ;
   }
   else {
	   print "$token\n";
    }
}
