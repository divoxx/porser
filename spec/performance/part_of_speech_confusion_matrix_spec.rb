require File.dirname(__FILE__) + "/../spec_helper"

describe Performance::PartOfSpeechConfusionMatrix do
  context "perfect parsing" do
    before :each do
      @gold_sentence   = "(S (NP (ART Um) (N revivalismo) (ADJP (ADJ refrescante))))"
      @parsed_sentence = "(S (NP (ART Um) (N revivalismo) (ADJP (ADJ refrescante))))"
      @matrix          = Performance::PartOfSpeechConfusionMatrix.new
      @matrix.account(@gold_sentence, @parsed_sentence)
    end
  
    it "should have 100% of correctness" do
      @matrix.correctness.should == 1.0
    end
    
    it "should have 0% of errorness" do
      @matrix.errorness.should == 0.0
    end
    
    it "should iterate over all possible results" do
      yields = [
        ["ADJ", "ADJ", 1.0],
        ["ADJ", "ART", 0.0],
        ["ADJ", "N",   0.0],
        ["ART", "ADJ", 0.0],
        ["ART", "ART", 1.0],
        ["ART", "N",   0.0],
        ["N",   "ADJ", 0.0],
        ["N",   "ART", 0.0],
        ["N",   "N",   1.0]
      ]
      
      results = []
      
      @matrix.each do |expected, got, percent|
        results << [expected, got, percent]
      end
      
      results.should == yields
    end
  end
  
  context "totally wrong parsing" do
    before :each do
      @gold_sentence   = "(S (NP (ART Um) (N revivalismo) (ADJP (ADJ refrescante))))"
      @parsed_sentence = "(S (NP (N Um) (ART revivalismo) (ADJP (CONJ refrescante))))"
      @matrix          = Performance::PartOfSpeechConfusionMatrix.new
      @matrix.account(@gold_sentence, @parsed_sentence)
    end
  
    it "should have 0% of correctness" do
      @matrix.correctness.should == 0.0
    end
    
    it "should have 100% of errorness" do
      @matrix.errorness.should == 1.0
    end
    
    it "should iterate over all possible results" do
      yields = [
        ["ADJ",  "ADJ",  0.0],
        ["ADJ",  "ART",  0.0],
        ["ADJ",  "CONJ", 1.0],
        ["ADJ",  "N",    0.0],
        ["ART",  "ADJ",  0.0],
        ["ART",  "ART",  0.0],
        ["ART",  "CONJ", 0.0],
        ["ART",  "N",    1.0],
        ["CONJ", "ADJ",  1.0],
        ["CONJ", "ART",  1.0],
        ["CONJ", "CONJ", 1.0],
        ["CONJ", "N",    1.0],
        ["N",    "ADJ",  0.0],
        ["N",    "ART",  1.0],
        ["N",    "CONJ", 0.0],
        ["N",    "N",    0.0]
      ]
      
      results = []
      
      @matrix.each do |expected, got, percent|
        results << [expected, got, percent]
      end
      
      results.should == yields
    end
  end
end