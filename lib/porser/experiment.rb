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
    
    def filters
      unless @filters
        token_list = File.basename(@path).split("-")[1..-1]
        @filters = token_list == ['unchanged'] ? [] : token_list.map { |filter_name| puts filter_name }
      end
      @filters
    end
    
    def train!(heap_size = 700)
      cmd = "/usr/bin/env java"
      cmd << " -Xms#{heap_size}\\m -Xmx#{heap_size}\\m"
      cmd << " -cp \"#{Porser.java_classpath}:#{@path}\""
      cmd << " -Ddanbikel.parser.Model.printPrunedEvents=false"
      cmd << " -Dparser.settingsDir=\"#{@path}\""
      cmd << " -Dparser.settingsFile=\"#{settings_path.check!}\""
      cmd << " danbikel.parser.Trainer"
      cmd << " -i #{gold_path_for(:train).check!} -o #{observed_path} -od #{objects_path}"
      cmd << " > #{log_path_for(:train)} 2>&1"
      `#{cmd}`
    ensure
      `rm -rf #{Porser.path.join('*.prune-log')}`
    end
    
    def generate_corpus!
      copy_proc = Proc.new do |path|
        File.open(path, "r") do |infp|
          File.open(@path.join("corpus.#{File.basename(path)}"), "w") do |outfp|
            while line = infp.gets
              outfp.write(line)
            end
          end
        end
      end
      
      @selection.parseable_paths.each(&copy_proc)
      @selection.gold_paths.each(&copy_proc)
    end
    
    def parseable_path_for(what)
      @path.join("corpus.#{what}.parseable.txt")
    end
    
    def gold_path_for(what)
      @path.join("corpus.#{what}.gold.txt")
    end
    
    def log_path_for(what)
      @path.join("log.#{what}.txt")
    end
    
    def head_rules_path
      @path.join('head-rules.lisp')
    end
    
    def score_path
      @path.join('score.txt')
    end
        
    def objects_path
      @path.join('objects.gz')
    end
    
    def observed_path
      @path.join('observed.gz')
    end
    
    def settings_path
      @path.join('settings.properties')
    end
  end
end