require 'porser/cli/components/question'
require 'porser/cli/components/file_list'
require 'porser/cli/actions/selection'

namespace :corpus do
  namespace :selections do
    def selections_path
      Porser.path.join("corpus", "selections")
    end
    
    desc "Create a selection of a corpus splitted in train, dev and test samples"
    task :create do
      corpus_path = Porser.path.join("corpus", "pre-processed")
      file_list   = Porser::CLI::FileList.new(corpus_path, :title => "Available corpus", :question => "Select the corpus you want to use")
      
      if corpora_path = file_list.ask
        Porser::CLI::Selection.run(corpora_path)
      else
        puts "None selected, aborting."
      end
    end
    
    desc "Remove all foders in corpus/selections/"
    task :clear do
      question = Porser::CLI::Question.new("Are you sure you wanna remove corpus/selections/* ?", :default => "n")

      if question.ask
        selections_path = Porser.path.join("corpus", "selections")
        Dir["#{selections_path}/*"].each { |path| rm_rf(path) }
        puts "Removed."
      else
        puts "Aborted."
      end
    end
  end
end