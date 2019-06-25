package graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Stack;

public class Manager {
	ArrayList<Edge> tour;
	ArrayList<Stack<Edge>> stacks;

	public Manager(ArrayList<Edge> tour, ArrayList<Stack<Edge>> stacks) {
		this.tour = tour;
		this.stacks = stacks;
	}

	public Stack<Edge> recycle() {
		Stack<Edge> cycle = new Stack<Edge>();

		HashSet<Integer> visited = new HashSet<Integer>();

		for (Edge edge : this.tour) {
			if (!visited.contains(edge.getFrom())) {
				visited.add(edge.getFrom());
				Stack<Edge> tmp = new Stack<Edge>();
				tmp.add(edge);
				while (tmp.peek().getTo() != edge.getFrom()) {
					Edge toPush = null;
					for (Edge walk : this.tour) {
						if (walk.getFrom() == tmp.peek().getTo()) {
							toPush = walk;
							break;
						}
					}

					if (toPush == null) {
						for (int i = 0; i < this.stacks.size(); i++) {
							Stack<Edge> toRelief = this.stacks.get(i);
							if (toRelief.contains(tmp.peek()) || toRelief.contains(edge)) {
								this.stacks.remove(toRelief);
							}
						}
						break;
					}
					visited.add(toPush.getFrom());
					tmp.push(toPush);
				}

				if (cycle.size() == 0 || tmp.size() < cycle.size()) {
					cycle.clear();
					cycle.addAll(tmp);
				}
			}
		}
		return cycle;
	}

//	private double getRouteLength(ArrayList<Edge> tour, ArrayList<City> data) {
//		double distance = 0;
//		for (Edge edge : tour) {
//			City from = data.get(edge.getFrom());
//			City to = data.get(edge.getTo());
//			distance += pointDistance(from.getX(), from.getY(), to.getX(), to.getY());
//		}
//		return distance;
//	}

	private double pointDistance(double p1x, double p1y, double p2x, double p2y) {
		return Math.sqrt(Math.pow(p1x - p2x, 2) + Math.pow(p1y - p2y, 2));
	}

	public double[][] getAdjMatrix(ArrayList<City> data) {
		double[][] matrix = new double[data.size()][data.size()];

		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix.length; j++) {
				City from = data.get(i);
				City to = data.get(j);

				matrix[i][j] = pointDistance(from.getX(), from.getY(), to.getX(), to.getY());
			}
		}

		return matrix;
	}
}
