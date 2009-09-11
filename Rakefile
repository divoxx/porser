require File.dirname(__FILE__) + "/lib/porser"
require 'rake'

Porser.require_all!
include Porser

Dir["#{Porser.path.join('lib', 'tasks')}/*.rake"].each { |task_path| load(task_path) }