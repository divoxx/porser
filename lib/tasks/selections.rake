namespace :selections do
  desc "Create a selection of a corpus splitted in train, dev and test samples"
  task :create do
    corpus_path = Porser.path.join("corpus", "pre-processed")
    file_list   = CLI::Components::FileList.new(corpus_path, :title => "Available corpus", :question => "Select the corpus you want to use")
    
    if corpora_path = file_list.ask
      Selection.create!(corpora_path)
      puts "Done."
    else
      puts "None selected, aborting."
    end
  end
  
  desc "Remove all foders in corpus/selections/"
  task :clear do
    question = CLI::Components::Question.new("Are you sure you wanna remove corpus/selections/* ?", :default => "n")

    if question.ask
      selections_path = Porser.path.join("corpus", "selections")
      Dir["#{selections_path}/*"].each { |path| rm_rf(path) }
      puts "Cleared."
    else
      puts "Aborted."
    end
  end
end