module Porser
  module Filters
    class LematizeAllAndAppendCategoryOnlyToVerbUsingTreeTagger
      def run(sentence)
        path   = Porser.path.join('vendor', 'tree-tagger')
        bin    = path.join('bin', 'tree-tagger')
        args   = "-lemma #{path.join('lib', 'pt.par')}"
        output = `echo "#{sentence.word_line_string.gsub('"', '\"')}" | iconv -f UTF8 -t ISO-8859-1 | #{bin} #{args} 2> /dev/null | iconv -f ISO-8859-1 -t UTF8`.split(/\n/)
        
        index = 0
        sentence.each do |node, range|
          if node.is_a?(Corpus::PartOfSpeech)
            lemma = output[index].split(/\t/)[1].strip
            
            unless lemma == '<unknown>'
              node.word = output[index].split(/\t/)[1]
              node.word << "_#{node.tag}" if node.tag =~ /^V_/
            end
            
            index += 1
          end
        end
        
        sentence
      end
    end
  end
end