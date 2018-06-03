package de.ovgu.featureide.oscar.model;

public class Node {
	
	private Feature father;
	private Feature child;
	
	public Node(Feature father, Feature child) {
		super();
		this.father = father;
		this.child = child;
	}
	
	public Feature getFather() {
		return father;
	}
	
	public Feature getChild() {
		return child;
	}
	
	public Feature[] getSuccessors(){
		return child.getChildren().toArray(new Feature[child.getChildren().size()]);
	}

}
