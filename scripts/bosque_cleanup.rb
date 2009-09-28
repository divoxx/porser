#!/usr/bin/env ruby

unless RUBY_VERSION =~ /^1.9/
  puts "Ruby >= 1.9 is required"
  exit
end

if ARGV.length != 2
  puts "Usage: #{$0} <original> <output>"
  exit
end

in_comment = false
in_tag     = false
in_frase   = false
substr     = ""
original   = File.open(ARGV[0], "r:ISO-8859-1")
output     = File.open(ARGV[1], "w:UTF-8")

original.each_char do |char|
  if in_comment
    if char == "\n" then
      substr = ""
      in_comment = false
    end
    next
  end

  if char == " " && (substr[-1] == " " || substr[-1] == "\n")
    next
  else
    substr += (case char when "\n" then " " when "-" then "_" else char end).chr
  end

  case substr
  when /\A(.*)#\z/m then
    in_comment = true
    output.write $1
    substr = ""

  when /\A(.*)\(FRASE[^\(]+\(\z/m then
    output.write "#{$1}\n\(S "
    substr = "("

  when /\A(.*)\((?!FRASE)([^\s]+)\s\z/m then
    output.write "#{$1}("
    output.write $2.include?(":") ? $2.split(":")[1].upcase.gsub(/[^a-zA-Z_]+/, '').gsub(/_$/, '') : $2
    output.write " "
    substr = ""

  when /\A(.*)\(([^\s]+)\)\z/m then
    output.write "#{$1}(#{$2} #{$2})"
    substr = ""

  when /\A(.*?)[\+_]?\)\z/m then
    output.write "#{$1})"
    substr = ""
  end
end

output.write substr
