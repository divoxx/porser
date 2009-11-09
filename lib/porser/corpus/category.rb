module Porser
  module Corpus
    class Category
      include Enumerable
      
      attr_reader :tag, :children
      
      def initialize(tag, children = [])
        @tag      = tag
        @children = children
      end
      
      def [](lookup_range)
        find_all { |node, range| range == lookup_range }.map { |node, range| node }
      end
      
      def each(index = 0, &block)
        start_index = index
        
        if @children.empty?
          index += 1
        else
          @children.each do |child|
            index = child.each(index, &block)
          end
        end
        
        yield(self, start_index..index)
        index
      end
      
      def <<(node)
        @children << node
      end
      
      def pretty_string(level = 0)
        children_str = @children.map { |child| child.pretty_string(level + 1) }.join(" ")
        indent_str   = "  " * level
        line_break   = "\n" unless level == 0
        "#{line_break}#{indent_str}(#{@tag}#{children_str})"
      end
      
      def to_s
        children_str = @children.map { |child| child.to_s }.join(" ")
        "(#{@tag} #{children_str})"
      end
    end
  end
end