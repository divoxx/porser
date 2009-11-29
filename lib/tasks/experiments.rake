namespace :experiments do
  def ask_experiment_path(multiple = false)
    experiments_file_list = CLI::Components::FileList.new(Porser.path.join('corpus', 'experiments'), :title => "Available experiments", :question => "Choose the experiment to train", :only_folder => true, :multiple => multiple)
    experiments_file_list.ask or puts "No experiment selected, aborting" && exit(1)
  end
  
  desc "Create a new experiment"
  task :create do
    filters    = CLI::Components::FileList.new(Porser.path.join('lib', 'porser', 'filters'), :title => "Available filters", :question => "Choose the filters to apply", :multiple => true, :full_path => false, :allow_none => true).ask
    experiment = Experiment.create!(filters)
    puts "Experiment created at #{experiment.path}"
    
    print "\nCopy config files from sample/ to the experiment directory? "
    
    if $stdin.gets.chomp.downcase == "y"
      `cp samples/* #{experiment.path}`
      puts "Copied."
    end
  end
  
  desc "Rebuild a corpus experiment"
  task :rebuild do
    ask_experiment_path(true).map { |path| Experiment.new(path) }.each do |experiment|
      puts "* #{experiment.path}"
      experiment.generate_corpus!
      puts "Done."
    end
  end
  
  desc "Run the training process for an experiment"
  task :train do
    experiment = Experiment.new(ask_experiment_path)
    puts "Training..."
    experiment.train!
    puts "Done."
    exec("less #{experiment.log_path_for(:train, :train)}")
  end
  
  desc "Run the parsing process for an experiment"
  task :parse do
    experiment = Experiment.new(ask_experiment_path)
    puts "Parsing..."
    experiment.parse!(:dev)
    puts "Done."
    exec("less #{experiment.log_path_for(:parse, :dev)}")
  end
  
  file 'vendor/scorer/evalb' => 'vendor/scorer/evalb.c' do |t|
    `cd vendor/scorer && make`
  end
  
  desc "Run the scoring process for an experiment"
  task :score => 'vendor/scorer/evalb' do
    experiment = Experiment.new(ask_experiment_path)
    puts "Scoring..."
    experiment.score!(:dev)
    puts "Done."
    exec("less #{experiment.score_path_for(:dev)}")
  end
  
  desc "Run the quantitative scoring process for an experiment"
  task :score_confusion do
    experiment = Experiment.new(ask_experiment_path)
    puts "Building confusion matrices..."
    experiment.score_confusion!(:dev)
    puts "Done."
    exec("less #{experiment.score_confusion_path_for(:dev)}")
  end
  
  desc "Generate the LaTeX documentation for the experiment"
  task :doc do
    experiment = Experiment.new(ask_experiment_path)
    puts "Scoring..."
    experiment.document!(:dev)
    puts "Done."
    exec("less #{experiment.documentation_path_for(:dev)}")
  end
  
  desc "Run the whole process for many experiments"
  task :run => 'vendor/scorer/evalb' do
    experiments = ask_experiment_path(true).map { |path| Experiment.new(path) }
    
    experiments.each do |experiment|
      $stdout.puts "Running experiment #{experiment.path}"
      $stdout.print " * Training..."
      $stdout.flush
      experiment.train!
      $stdout.puts "Done."
      $stdout.flush
      
      $stdout.print " * Parsing..."
      $stdout.flush
      experiment.parse!(:dev)
      $stdout.puts "Done."
      $stdout.flush
      
      $stdout.print " * Scoring..."
      $stdout.flush
      experiment.score!(:dev)
      $stdout.puts "Done."
      $stdout.flush
      
      $stdout.print " * Building confusion matrices..."
      $stdout.flush
      experiment.score_confusion!(:dev)
      $stdout.puts "Done."
      $stdout.flush
      
      $stdout.print " * Generating LaTeX documentation..."
      $stdout.flush
      experiment.document!(:dev)
      $stdout.puts "Done."
      $stdout.flush
    end
  end
  
  desc "Prettyprint"
  task :pretty_print, :what do |t, args|
    experiment = Experiment.new(ask_experiment_path)
    experiment.pretty_print!(args.what || "dev")
  end
end