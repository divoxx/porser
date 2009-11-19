module Porser
  module Filters
    class LematizeAllAndAppendCategoryOnlyToVerb
      def run(sentence)
        map(sentence.root_node)
        sentence
      end
    
    protected
      def map(node)
        case node
        when Corpus::Category
          Corpus::Category.new(node.tag, node.children.map { |child| map(child) })
        when Corpus::PartOfSpeech
          if node.extra[:lema]
            if node.tag =~ /^V/
              word = "#{node.extra[:lema]}_#{node.tag}"
            else
              word = node.extra[:lema]
            end
          else
            word = node.word
          end
          Corpus::PartOfSpeech.new(node.tag, word, node.extra)
        end
      end
    end
  end
end