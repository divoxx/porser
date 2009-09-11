require File.dirname(__FILE__) + "/../spec_helper"
require 'porser/cli/question'

describe Porser::CLI::Question do
  context 'without default answer' do
    before :each do
      @str      = "Are you sure?"
      @input    = mock(:input, :null_object => true)
      @output   = mock(:output, :null_object => true)
      @question = Porser::CLI::Question.new(@str, :input => @input, :output => @output)
    end

    it "should return true" do
      @input.should_receive(:gets).and_return("y\n")
      @question.ask.should == true
    end
  end
end