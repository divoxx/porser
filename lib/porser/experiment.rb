module Porser
  class Experiment
    attr_reader :path
    
    def self.create!(selection, filters = [])
      filter_str = filters.empty? ? 'unchanged' : filters.join("-")
      experiment = new(selection.path.join("#{Time.now.utc.strftime(TimeFormat)}-#{filter_str}"))
      Dir.mkdir(experiment.path)
      experiment.generate_corpus!      
      experiment
    end
    
    attr_reader :path, :selection
    
    def initialize(path)
      @path      = Pathname.new(path.to_s)
      @selection = Selection.new(File.dirname(@path))
    end
    
    def name
      self.path.basename
    end
    
    def filters
      unless @filters
        token_list = File.basename(@path).split("-")[1..-1]
        @filters = token_list.map { |filter_name| Filters.const_get(filter_name.camelize).new unless filter_name == 'unchanged' }.compact
      end
      @filters
    end
    
    def train!(what = :train, heap_size = 1400)
      cmd = "/usr/bin/env java"
      cmd << " -Xms#{heap_size}\\m -Xmx#{heap_size}\\m"
      cmd << " -cp \"#{Porser.java_classpath}:#{@path}\""
      cmd << " -Ddanbikel.parser.Model.printPrunedEvents=false"
      cmd << " -Dparser.settingsDir=\"#{@path}\""
      cmd << " -Dparser.settingsFile=\"#{settings_path.check!}\""
      cmd << " danbikel.parser.Trainer"
      cmd << " -i #{gold_path_for(what).check!} -o #{observed_path} -od #{objects_path}"
      cmd << " > #{log_path_for(:train, what)} 2>&1"
      `#{cmd}`
    ensure
      `rm -rf #{Porser.path.join('*.prune-log')}`
    end
    
    def parse!(what = :dev, heap_size = 1400)
      cmd = "/usr/bin/env java"
      cmd << " -Xms#{heap_size}\\m -Xmx#{heap_size}\\m"
      cmd << " -cp \"#{Porser.java_classpath}:#{@path}\""
      cmd << " -Ddanbikel.parser.Model.printPrunedEvents=false"
      cmd << " -Dparser.settingsDir=\"#{@path}\""
      cmd << " -Dparser.settingsFile=\"#{settings_path.check!}\""
      cmd << " danbikel.parser.Parser"
      cmd << " -is #{objects_path} -sa #{parseable_path_for(what)}"
      cmd << " > #{log_path_for(:parse, what)} 2>&1"
      `#{cmd}`
    end
    
    def score!(what = :dev)
      cmd = "/usr/bin/env java -Xms200m -Xmx200m -cp \"#{Porser.java_classpath}:#{@path}\" danbikel.parser.util.AddFakePos #{gold_path_for(what)} #{parsed_path_for(what)} > #{scorable_file_for(what)} 2> #{log_path_for(:score, what)}"
      cmd << " &&"
      cmd << " ./vendor/scorer/evalb -p vendor/scorer/BIKEL.prm #{gold_path_for(what)} #{scorable_file_for(what)} > #{score_path_for(what)} 2>&1"
      `#{cmd}`
    end
    
    def score_confusion!(what = :dev)
       File.open(gold_path_for(what), "r") do |gold_fp|
         File.open(parsed_path_for(what), "r") do |parsed_fp|
           File.open(score_confusion_path_for(what), "w") do |conf_fp|
             pos_matrix = Performance::PartOfSpeechConfusionMatrix.new
             cat_matrix = Performance::CategoryConfusionMatrix.new
             
             while gold = gold_fp.gets and parsed = parsed_fp.gets
               pos_matrix.account(gold, parsed)
               cat_matrix.account(gold, parsed)
              end
              
             conf_fp.write(<<-EOF)
Part Of Speech Matrix (#{"%.2f" % pos_matrix.correctness} correctness): 
#{pos_matrix.pretty_string}

Sintatic Matrix (#{"%.2f" % cat_matrix.correctness} correctness):
#{cat_matrix.pretty_string}
             EOF
           end
         end
       end
    end
    
    def document!(what = :dev)
      template = ERB.new(File.read(Porser.path.join('lib', 'templates', 'experiment.tex.erb')))
      File.open(documentation_path_for(what), "w") { |fp| fp.write(template.result(binding)) } 
    end
    
    def generate_corpus!
      @selection.corpus_paths.each do |path|
        File.open(path, "r") do |infp|
          File.open(@path.join(File.basename(path).gsub(/^corpus\.(.*?)\.txt$/, 'corpus.\1.parseable.txt')), "w") do |parseable_outfp|
            File.open(@path.join(File.basename(path).gsub(/^corpus\.(.*?)\.txt$/, 'corpus.\1.gold.txt')), "w") do |gold_outfp|
              corpus = $1.to_sym
              
              while line = infp.gets
                sentence = Corpus::Sentence.parse(line)
                filters.each do |filter|
                  method = filter.method(:run)
                  
                  if method.arity == 2
                    sentence = method.call(sentence, corpus)
                  else
                    sentence = method.call(sentence)
                  end
                end
                
                gold_outfp.write("#{sentence}\n")
                parseable_outfp.write(sentence.to_s.gsub(/\([^\s]+|\)/, "").gsub(/^\s*(.*)\s*$/, "(\\1)\n").squeeze(" "))
              end
            end
          end
        end
      end
    end
    
    def head_find_rules
      @head_find_rules ||= head_rules_path.readlines.reject { |l| l =~ /^(;|\s*$)/ }.join
    end
    
    def settings
      @settings ||= settings_path.read
    end
    
    def score(what = :dev)
      @score ||= score_path_for(what).read
    end
    
    def documentation_path_for(what)
      @path.join("document.#{what}.tex")
    end
    
    def parseable_path_for(what)
      @path.join("corpus.#{what}.parseable.txt")
    end
    
    def gold_path_for(what)
      @path.join("corpus.#{what}.gold.txt")
    end
    
    def parsed_path_for(what)
      "#{parseable_path_for(what)}.parsed"
    end
    
    def log_path_for(action, what)
      @path.join("log.#{action}.#{what}.txt")
    end
    
    def scorable_file_for(what)
      "#{parseable_path_for(what)}.scorable"
    end
    
    def head_rules_path
      @path.join('head-rules.lisp')
    end
    
    def score_path_for(what)
      @path.join("score.#{what}.txt")
    end
    
    def score_confusion_path_for(what)
      @path.join("score_confusion.#{what}.txt")
    end
        
    def objects_path
      @path.join("objects.gz")
    end
    
    def observed_path
      @path.join("observed.gz")
    end
    
    def settings_path
      @path.join('settings.properties')
    end
  end
end
