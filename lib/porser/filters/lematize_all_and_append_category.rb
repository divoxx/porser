module Porser
  module Filters
    class LematizeAllAndAppendCategory
      def run(sentence)
        Corpus::Sentence.new(map(sentence.root_node))
      end
    
    protected
      def map(node)
        case node
        when Corpus::Category
          Corpus::Category.new(node.tag, node.children.map { |child| map(child) })
        when Corpus::PartOfSpeech
          word = node.extra[:lema] ? "#{node.extra[:lema]}_#{node.tag}" : node.word
          Corpus::PartOfSpeech.new(node.tag, word, node.extra)
        end
      end
    end
  end
end