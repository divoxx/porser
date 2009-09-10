#!/usr/bin/env ruby
require File.dirname(__FILE__) + "/../lib/porser"
require 'trollop'
require 'porser/cli/selection'

opts = Trollop.options do
  banner <<-BANNER
Usage: #{$0} <path_to_corpus>
  BANNER
  
  if ARGV.empty? || ARGV.size > 1
    educate
    exit(1)
  end
end

begin
  Porser::CLI::Selection.run(ARGV[0])
rescue ArgumentError => e
  puts "ERROR: #{e.message}"
  puts ""
  puts e.backtrace
end