require 'spec/rake/spectask'

desc "Run all examples"
Spec::Rake::SpecTask.new do |t|
  t.spec_opts  = ['--options', "\"#{Porser.path.join('spec', 'spec.opts')}\""]
  t.spec_files = FileList['spec/**/*_spec.rb']
  t.rcov       = true
  t.rcov_opts  = ['--exclude spec/']
end