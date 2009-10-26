class Porser::Corpus::SentenceParser
rule
  sentence        : node            { @object = val[0] }
                  ;

  node            : category        { result = val[0] }
                  | part_of_speech  { result = val[0] }
                  ;
                  
  node_list       : node_list node  { result = val[0] << val[1] }
                  |                 { result = [] }
                  ;

  category        : '(' WORD node_list ')' { result = Category.new(val[1], val[2]) }
                  ;
           
  part_of_speech  : '(' WORD '-' WORD WORD ')' { result = PartOfSpeech.new(val[1], val[3], val[4]) }
                  ;
                                                    
end

---- header ----
require 'porser/corpus/sentence_tokenizer'

---- inner ----  
  def initialize(tokenizer_klass = SentenceTokenizer)
    @tokenizer_klass = tokenizer_klass
  end
  
  def parse(string)
    @tokenizer = @tokenizer_klass.new(string)
    yyparse(@tokenizer, :each)
    return @object
  ensure
    @tokenizer = nil
  end
  
  def next_token
    @tokenizer.next_token
  end