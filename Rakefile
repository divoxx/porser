require File.dirname(__FILE__) + "/lib/porser"
require 'rake'
require 'spec/rake/spectask'

desc "Run all examples"
Spec::Rake::SpecTask.new do |t|
  t.spec_opts  = ['--options', "\"#{Porser.path.join('spec', 'spec.opts')}\""]
  t.spec_files = FileList['spec/**/*_spec.rb']
end

namespace :corpus do
  namespace :selections do
    def selections_path
      Porser.path.join("corpus", "selections")
    end
    
    desc "Create a selection of a corpus splitted in train, dev and test samples"
    task :create do
      corpus_path = Porser.path.join("corpus", "pre-processed")
      options     = Dir["#{corpus_path}/*"]
      
      puts "Available corpus:"
      options.each_with_index do |opt, i|
        puts "  [#{"%1d" % i}] #{File.basename(opt)}"
      end
      
      print "Select the corpus you want to use: "
      selected_idx = Integer($stdin.gets) rescue ArgumentError
      
      if selected_idx
        require 'porser/cli/selection'
        Porser::CLI::Selection.run(options[selected_idx])
      else
        puts "None selected, aborting."
      end
    end
    
    desc "Remove all foders in corpus/selections/"
    task :clear do
      print "Are you sure you wanna remove corpus/selections/* ? [Y/n] "

      if $stdin.gets.downcase.chomp == "y"
        selections_path = Porser.path.join("corpus", "selections")
        Dir["#{selections_path}/*"].each { |path| rm_rf(path) }
        puts "Removed."
      else
        puts "Aborted."
      end
    end
  end
end