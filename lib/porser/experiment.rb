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
        @filters = token_list.map { |filter_name| Filters.const_get(filter_name.camelize) unless filter_name == 'unchanged' }.compact
      end
      @filters
    end
    
    def train!(what = :train, heap_size = 700)
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
    
    def parse!(what = :dev, heap_size = 700)
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
    
    def document!(what = :dev)
      template = ERB.new(File.read(Porser.path.join('lib', 'templates', 'experiment.tex.erb')))
      File.open(documentation_path_for(what), "w") { |fp| fp.write(template.result(binding)) } 
    end
    
    def generate_corpus!
      filter_runner = FilterRunner.new(*filters)
      
      copy_proc = Proc.new do |path|
        File.open(path, "r") do |infp|
          File.open(@path.join("corpus.#{File.basename(path)}"), "w") do |outfp|
            while line = infp.gets
              outfp.write(filter_runner.run(line))
            end
          end
        end
      end
      
      @selection.parseable_paths.each(&copy_proc)
      @selection.gold_paths.each(&copy_proc)
    end
    
    def head_find_rules
      @head_find_rules ||= head_rules_path.read.reject { |l| l =~ /^(;|\s*$)/ }
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