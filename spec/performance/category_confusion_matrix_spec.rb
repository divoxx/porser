require File.dirname(__FILE__) + "/../spec_helper"

describe Performance::CategoryConfusionMatrix do
  context "perfect parsing" do
    before :each do
      @gold_sentence   = "(S (NP (ART Um) (N revivalismo) (ADJP (ADJ refrescante))))"
      @parsed_sentence = "(S (NP (ART Um) (N revivalismo) (ADJP (ADJ refrescante))))"
      @matrix          = Performance::CategoryConfusionMatrix.new(@gold_sentence, @parsed_sentence)
    end

    it "should have 100% of correctness" do
      @matrix.correctness.should == 1.0
    end

    it "should have 0% of errorness" do
      @matrix.errorness.should == 0.0
    end

    it "should iterate over all possible results" do
      yields = [
        ["ADJ",   "ADJ",    1.0],
        ["ADJ",   "ADJP",   0.0],
        ["ADJ",   "ART",    0.0],
        ["ADJ",   "N",      0.0],
        ["ADJ",   "NP",     0.0],
        ["ADJ",   "S",      0.0],
        ["ADJP",  "ADJ",    0.0],
        ["ADJP",  "ADJP",   1.0],
        ["ADJP",  "ART",    0.0],
        ["ADJP",  "N",      0.0],
        ["ADJP",  "NP",     0.0],
        ["ADJP",  "S",      0.0],
        ["ART",   "ADJ",    0.0],
        ["ART",   "ADJP",   0.0],
        ["ART",   "ART",    1.0],
        ["ART",   "N",      0.0],
        ["ART",   "NP",     0.0],
        ["ART",   "S",      0.0],
        ["N",   "ADJ",      0.0],
        ["N",   "ADJP",     0.0],
        ["N",   "ART",      0.0],
        ["N",   "N",        1.0],
        ["N",   "NP",       0.0],
        ["N",   "S",        0.0],
        ["NP",  "ADJ",      0.0],
        ["NP",  "ADJP",     0.0],
        ["NP",  "ART",      0.0],
        ["NP",  "N",        0.0],
        ["NP",  "NP",       1.0],
        ["NP",  "S",        0.0],
        ["S",   "ADJ",      0.0],
        ["S",   "ADJP",     0.0],
        ["S",   "ART",      0.0],
        ["S",   "N",        0.0],
        ["S",   "NP",       0.0],
        ["S",   "S",        1.0]
      ]

      results = []

      @matrix.each do |expected, got, percent|
        results << [expected, got, percent]
      end

      results.should == yields
    end
  end
end