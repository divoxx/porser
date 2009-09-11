require File.dirname(__FILE__) + "/../../spec_helper"
require 'porser/cli/actions/selection'
include Porser::CLI::Actions

describe Selection do
  before :each do
    @file      = StringIO.new((1..10).map{ |n| "(S (N #{n}))" }.join("\n"))
    @file_path = "dummy/path"
    
    Dir.stub!(:mkdir)
    
    File.stub!(:open).and_return do |path, mode|
      if path == @file_path
        @file
      else
        mock("file_#{path}", :close => true, :write => nil)
      end
    end
    
    @time_mock = mock(:time, :strftime => "20090910111213")
    @time_mock.stub!(:utc).and_return(@time_mock)
    Time.stub!(:now).and_return(@time_mock)
    
    srand(0)
  end
  
  def run
    Selection.run(@file_path)
  end
  
  it "should create annotated train file" do
    file_mock = mock(:train_annotate_mock, :close => true)
    
    [1, 2, 5, 6, 7, 8, 9, 10].each do |line|
      file_mock.should_receive(:write).with("(S (N #{line}))\n").ordered
    end
    
    File.should_receive(:open).with(Porser.path.join('corpus', 'selections', @time_mock.strftime, 'train.txt').to_s, "w").and_return(file_mock)
    
    run
  end
  
  it "should create annotated (gold) development file" do
    file_mock = mock(:dev_gold_file, :close => true)
    file_mock.should_receive(:write).with("(S (N 3))\n").ordered
    File.should_receive(:open).with(Porser.path.join('corpus', 'selections', @time_mock.strftime, 'devel.gold.txt').to_s, "w").and_return(file_mock)
    run
  end
  
  it "should create clean (parseable) development file" do
    file_mock = mock(:dev_parseable_file, :close => true)
    file_mock.should_receive(:write).with("(3)\n").ordered
    File.should_receive(:open).with(Porser.path.join('corpus', 'selections', @time_mock.strftime, 'devel.parseable.txt').to_s, "w").and_return(file_mock)
    run
  end
    
  it "should create annotated (gold) test file" do
    file_mock = mock(:test_gold_file, :close => true)
    file_mock.should_receive(:write).with("(S (N 4))\n").ordered
    File.should_receive(:open).with(Porser.path.join('corpus', 'selections', @time_mock.strftime, 'test.gold.txt').to_s, "w").and_return(file_mock)
    run
  end
  
  it "should create clean test file" do
    file_mock = mock(:test_parseable_file, :close => true)
    file_mock.should_receive(:write).with("(4)\n").ordered
    File.should_receive(:open).with(Porser.path.join('corpus', 'selections', @time_mock.strftime, 'test.parseable.txt').to_s, "w").and_return(file_mock)
    run
  end
end