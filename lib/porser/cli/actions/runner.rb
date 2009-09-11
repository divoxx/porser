require 'porser/selection'
require 'porser/cli/actions/base'

module Porser
  module CLI
    module Actions
      class Runner < Base
        def initialize(selection_timestamp, heap_size, *args)
          @selection = Selection.find(selection_timestamp)
          @heap_size = heap_size
          super(*args)
        end
        
        def run!
          train!
        end
        
        def train!
          info "Training parser...\n"
          cmd = "/usr/bin/env java"
          cmd << " -Xms#{@heap_size}\\m -Xmx#{@heap_size}\\m"
          cmd << " -cp \"#{Porser.java_classpath}:#{@selection.path}\""
          cmd << " -Ddanbikel.parser.Model.printPrunedEvents=false"
          cmd << " -Dparser.settingsDir=\"#{@selection.path.check!}\""
          cmd << " -Dparser.settingsFile=\"#{@selection.settings_path.check!}\""
          cmd << " danbikel.parser.Trainer"
          cmd << " -i #{@selection.train_path.check!} -o #{@selection.observed_path} -od #{@selection.objects_path}"
          cmd << " > #{@selection.train_log_path}"
          cmd << "; rm -rf *.pune-log"
          info "#{cmd}\n"; `#{cmd}`
          info "Done.\n"
        end
      end
    end
  end
end