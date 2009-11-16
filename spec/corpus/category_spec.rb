require File.dirname(__FILE__) + "/../spec_helper"

describe Corpus::Category do
  before :each do
    @root_category = Corpus::Category.new('0', [
      Corpus::Category.new('1', [
        Corpus::Category.new('2'),
        Corpus::Category.new('3', [
          Corpus::Category.new('4'),
          Corpus::Category.new('5')
        ])
      ]),
      Corpus::Category.new('6', [
        Corpus::Category.new('7')
      ])
    ])
  end
  
  it "should allow iterating over the tree with index range" do
    expected = [
      ['2', 0..1],
      ['4', 1..2],
      ['5', 2..3],
      ['3', 1..3],
      ['1', 0..3],
      ['7', 3..4],
      ['6', 3..4],
      ['0', 0..4]
    ]
    
    results = []
    @root_category.each { |node, range| results << [node.tag, range] }
    
    results.should == expected
  end
  
  it "should allow iterating over the ranges" do
    expected = [
      [0..1, ['2']],
      [1..2, ['4']],
      [2..3, ['5']],
      [1..3, ['3']],
      [0..3, ['1']],
      [3..4, ['7', '6']],
      [0..4, ['0']]
    ]
    
    results = []
    @root_category.each_range { |range, nodes| results << [range, nodes.map { |n| n.tag }] }
    results.should == expected
  end
  
  it "should allow indexing the tree path" do
    nodes = @root_category[1..3]
    nodes.should_not be_empty
    nodes.size.should == 1
    nodes[0].tag.should == "3"
  end
  
  it "should return tag ranges ordered" do
    expected = [
      [0..1, '2'],
      [0..3, '1'],
      [0..4, '0'],
      [1..2, '4'],
      [1..3, '3'],
      [2..3, '5'],
      [3..4, '6'],
      [3..4, '7']
    ]
    
    @root_category.tag_ranges.should == expected
  end
end