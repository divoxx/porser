module Porser
  module Filters
    class LematizeOnlyVerb
      def run(sentence)
        map(sentence.root_node)
      end
    
    protected
      def map(node)
        case node
        when Corpus::Category
          Corpus::Category.new(node.tag, node.children.map { |child| map(child) })
        when Corpus::PartOfSpeech
          word = (node.tag =~ /^V/ ? node.extra[:lema] || node.word : node.word)
          Corpus::PartOfSpeech.new(node.tag, word, node.extra)
        end
      end
    end
  end
end