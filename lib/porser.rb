require 'pathname'

module Porser  
  TimeFormat = "%Y.%m.%d_%H.%M.%S" unless defined?(TimeFormat)
  
  def self.include_paths
    unless @include_paths
      @include_paths = []
      @include_paths << path.join("lib")
      @include_paths += path.join('vendor').children.map { |d| d.join('lib') }
    end
    @include_paths
  end
  
  def self.path
    @path ||= Pathname.new(File.expand_path(File.join(File.dirname(__FILE__), "..")))
  end
  
  def self.java_classpath
    paths = [java_ext_build_path, path.join('vendor', 'dbparser.jar')]
    paths.join(':')
  end
  
  def self.java_ext_path
    path.join('ext')
  end
  
  def self.java_ext_src_path
    java_ext_path.join('src')
  end
  
  def self.java_ext_build_path
    java_ext_path.join('build')
  end
  
  def self.boot!
    include_paths.each { |path| $:.unshift(path.to_s) }
  end
  
  def self.require_all!
    require 'porser/experiment'
    require 'porser/cli/components/file_list'
    require 'porser/cli/components/file_list'
    require 'porser/cli/components/question'
    require 'porser/corpus/category'
    require 'porser/corpus/part_of_speech'
    require 'porser/corpus/sentence_tokenizer'
    require 'porser/corpus/sentence_parser'
    require 'porser/corpus/sentence'
    require 'porser/performance/confusion_matrix'
    require 'porser/performance/category_confusion_matrix'
    require 'porser/performance/part_of_speech_confusion_matrix'    
    Dir["#{path.join('lib', 'filters')}/*.rb"].each { |path| require(path) }
  end
end

Porser.boot!

require 'active_support'
require 'erb'

Dir["#{Porser.path.join('lib', 'porser', 'filters')}/*"].each do |filter_path|
  require filter_path
end

# Monkey patches
class Pathname 
  def check
    if self.exist?
      self
    end
  end
  
  def check!
    self.check || raise("#{self} doesn't exist")
  end  
end

class Range
  include Comparable
  
  def <=>(other)
    min_order = self.min <=> other.min
    min_order == 0 ? self.max <=> other.max : min_order
  end
end