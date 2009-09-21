namespace :experiments do
  def ask_selection_path
    selections_path      = Porser.path.join("corpus", "selections")
    selections_file_list = CLI::Components::FileList.new(selections_path, :title => "Available selections", :question => "Choose the selection that contains the experiment", :only_folder => true)
    selections_file_list.ask or puts "No selection selected, aborting" && exit(1)
  end
  
  def ask_experiment_path(selection_path)
    experiments_file_list = CLI::Components::FileList.new(selection_path, :title => "Available experiments", :question => "Choose the experiment to train", :only_folder => true)
    experiments_file_list.ask or puts "No experiment selected, aborting" && exit(1)
  end
  
  desc "Create a new experiment from a selection"
  task :create do
    selection  = Selection.new(File.basename(ask_selection_path))
    filters    = CLI::Components::FileList.new(Porser.path.join('lib', 'porser', 'filters'), :title => "Available filters", :question => "Choose the filters to apply", :multiple => true, :full_path => false, :allow_none => true).ask
    experiment = Experiment.create!(selection, filters)
    puts "Experiment created at #{experiment.path}"
  end
  
  desc "Run the training process for a experiment"
  task :train do
    experiment = Experiment.new(ask_experiment_path(ask_selection_path))
    puts "Training..."
    experiment.train!
    puts "Done."
    exec("less #{experiment.log_path_for(:train)}")
  end
end