require 'pathname'

module Porser
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
end

Porser.include_paths.each { |path| $:.unshift(path) }