require File.dirname(__FILE__) + "/../spec_helper"

describe Corpus::Category do
  before :each do
    @root_category = Corpus::Category.new('root', [
      Corpus::Category.new('child_0', [
        Corpus::Category.new('child_0_0'),
        Corpus::Category.new('child_0_1')
      ]),
      Corpus::Category.new('child_1', [
        Corpus::Category.new('child_1_0')
      ])
    ])
  end
  
  it "should allow indexing the tree path" do
    @root_category[1,0].tag.should == 'child_1_0'
  end
  
  it "should iterate yielding index" do
    yields = []
    
    @root_category.each_node do |child, idx|
      yields << [child.tag, idx]
    end
    
    yields.should == [
      ['child_0',   [0]],
      ['child_0_0', [0,0]],
      ['child_0_1', [0,1]],
      ['child_1',   [1]],
      ['child_1_0', [1,0]],
    ]
  end
end