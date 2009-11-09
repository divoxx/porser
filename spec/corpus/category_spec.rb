require File.dirname(__FILE__) + "/../spec_helper"

describe Corpus::Category do
  before :each do
    @root_category = Corpus::Category.new('tree_0_4', [
      Corpus::Category.new('tree_0_3', [
        Corpus::Category.new('leaf_0_1'),
        Corpus::Category.new('tree_1_3', [
          Corpus::Category.new('leaf_1_2'),
          Corpus::Category.new('leaf_2_3')
        ])
      ]),
      Corpus::Category.new('leaf_3_4')
    ])
  end
  
  it "should allow iterating over the tree with index range" do
    expected = [
      ['leaf_0_1', 0..1],
      ['leaf_1_2', 1..2],
      ['leaf_2_3', 2..3],
      ['tree_1_3', 1..3],
      ['tree_0_3', 0..3],
      ['leaf_3_4', 3..4],
      ['tree_0_4', 0..4]
    ]
    
    results = []
    @root_category.each { |node, range| results << [node.tag, range] }
    
    results.should == expected
  end
  
  it "should allow indexing the tree path" do
    node = @root_category[1..3]
    node.should_not be_nil
    node.tag.should == "tree_1_3"
  end
end