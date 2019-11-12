#!/usr/bin/perl

local $/ = "-----END CERTIFICATE-----\n";

my $seen = {};

my $certificate_number = 1;
while (<>) {
    s/.*?(?=-----BEGIN CERTIFICATE-----)//s;
    if (/-----BEGIN CERTIFICATE-----/ and /-----END CERTIFICATE-----/) {
        if (not $seen->{$_}) {
            open(FH, "> certificate-${certificate_number}.crt") or die $!;
            print FH or die $!;
            close(FH) or die $!;
            $certificate_number++;
            $seen->{$_} = 1;
        }
    }
}
