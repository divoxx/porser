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
      
      def tag_ranges
        ranges = []
        # TODO: Don't exclude POS here
        each { |node, range| ranges << [range, node.tag] unless node.is_a?(Corpus::PartOfSpeech) }
        ranges.sort!
      end
      
      def each_range
        last_range = nil
        in_range   = []
        
        each do |node, range|
          if last_range != range
            yield(last_range, in_range) unless in_range.empty?
            in_range = []
          end
          
          in_range << node
          last_range = range
        end
        
        yield(last_range, in_range) unless in_range.empty?
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
      
      def clean_string
        @children.map { |child| child.clean_string }.join(" ")
      end
    end
  end
end