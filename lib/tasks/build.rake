desc "Compile and build the java extension"
task :build do
  mkdir_p(Porser.java_ext_build_path)
  cmd = "/usr/bin/env javac -cp \"#{Porser.java_classpath}\" -s \"#{Porser.java_ext_src_path.check!}\" -d \"#{Porser.java_ext_build_path}\" #{Porser.java_ext_src_path}/**/**.java"
  puts cmd
  `#{cmd}`
end