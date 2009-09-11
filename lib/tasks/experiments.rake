namespace :experiments do
  desc "Run the training process for a experiment"
  task :train do
    selections_path      = Porser.path.join("corpus", "selections")
    selections_file_list = CLI::Components::FileList.new(selections_path, :title => "Available selections", :question => "Choose the selection that contains the experiment", :only_folder => true)
    
    if selection_path = selections_file_list.ask
      experiments_file_list = CLI::Components::FileList.new(selection_path, :title => "Available experiments", :question => "Choose the experiment to train", :only_folder => true)
      
      if experiment_path = experiments_file_list.ask
        puts "Training..."
        experiment = Experiment.new(experiment_path)
        experiment.train!
        puts "Done."
        exec("less #{experiment.log_path_for(:train)}")
      else
        puts "No experiment selected, aborting."
      end        
    else
      puts "No selection selected, aborting."
    end
  end
end