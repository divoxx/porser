module Porser
  module Filters
    class LematizeAll
      def run(sentence)
        map(sentence.root_node)
      end
    
    protected
      def map(node)
        case node
        when Corpus::Category
          Corpus::Category.new(node.tag, node.children.map { |child| map(child) })
        when Corpus::PartOfSpeech
          Corpus::PartOfSpeech.new(node.tag, node.extra.delete(:lema) || node.word, node.extra)
        end
      end
    end
  end
end