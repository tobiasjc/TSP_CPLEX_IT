package app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import graph.City;
import graph.Edge;
import graph.Manager;
import graphics.MainFrame;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;

public class App {

	public static void main(String[] args) {
		HashMap<String, String> hashArgs = getHashArgs(args);

		FileManager fileManager = new FileManager(hashArgs);

		ArrayList<City> data = fileManager.readInstance();

		ArrayList<Edge> tour = new ArrayList<Edge>();
		ArrayList<Stack<Edge>> stacks = new ArrayList<Stack<Edge>>();
		Manager manager = new Manager(tour, stacks);

		double[][] distance = manager.getAdjMatrix(data);

		MainFrame mainFrame = new MainFrame(data, tour, distance, hashArgs.get("--imagePath"),
				Integer.parseInt(hashArgs.get("--index")));
		mainFrame.visualize();

		ArrayList<Edge> state = new ArrayList<Edge>();

		int count = 0;
		while (true) {
			try {
				IloCplex model = new IloCplex();

				// define variables
				IloIntVar[][] x = new IloIntVar[data.size()][data.size()];
				for (int i = 0; i < x.length; i++) {
					for (int j = 0; j < x.length; j++) {
						x[i][j] = model.boolVar("X[" + i + ", " + j + "]");
					}
				}

				// one has only a city to go, and should
				for (int i = 0; i < x.length; i++) {
					IloLinearIntExpr r = model.linearIntExpr();
					for (int j = 0; j < x.length; j++) {
						if (i == j)
							continue;
						r.addTerm(1, x[i][j]);
					}
					model.addEq(r, 1);
				}

				// one can only arrive to one city at a time, and should
				for (int j = 0; j < x.length; j++) {
					IloLinearIntExpr r = model.linearIntExpr();
					for (int i = 0; i < x.length; i++) {
						if (i == j)
							continue;
						r.addTerm(1, x[i][j]);
					}
					model.addEq(r, 1);
				}

				// one cannot go to the same city as he is
				for (int i = 0; i < x.length; i++) {
					IloLinearIntExpr r = model.linearIntExpr();
					r.addTerm(1, x[i][i]);
					model.addEq(r, 0);
				}

				// state
				for (Edge edge : state) {
					IloLinearIntExpr r = model.linearIntExpr();
					r.addTerm(1, x[edge.getFrom()][edge.getTo()]);
					model.addEq(r, 1);
				}

				// add cycle restrictions
				for (int i = 0; i < stacks.size(); i++) {
					ConstraintFactory constraintFactory = new ConstraintFactory();
					constraintFactory.cycleRestrictions(model, x, stacks.get(i));
				}

				// one should complete the tour within the smallest distance possible
				IloLinearNumExpr z = model.linearNumExpr();
				for (int i = 0; i < x.length; i++) {
					for (int j = 0; j < x.length; j++) {
						z.addTerm(distance[i][j], x[i][j]);
					}
				}
				model.addMinimize(z);

				tour.clear();

				if (model.solve()) {

					// get tour
					for (int i = 0; i < x.length; i++) {
						for (int j = 0; j < x.length; j++) {
							if (model.getValue(x[i][j]) >= 0.5) {
								tour.add(new Edge(i, j));
							}
						}
					}

					// repaint tour
					mainFrame.graphDraw.repaint();
				} else {
					System.err.println("Boi, u sick!");
				}

				System.out.println("Value = " + model.getObjValue());
				Stack<Edge> minimumTour = manager.recycle();
				if (minimumTour.size() == tour.size()) {
					break;
				} else if (minimumTour.size() < tour.size()) {
					stacks.add(minimumTour);
				}

				count++;

			} catch (IloException e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
				// TODO: handle exception
			}
		}
		System.out.println("With " + count + " iterations.");
		System.out.println("Done");
		mainFrame.repaint();
	}

//	private static boolean hasCrossing(ArrayList<Edge> tour, ArrayList<City> data, double bestDistance,
//			ArrayList<Stack<Edge>> stacks, ArrayList<Edge> state) {
//		ArrayList<Edge> toBan = new ArrayList<Edge>();
//		boolean response = false;
//		
//		try {
//			Thread.sleep((long) Float.MAX_VALUE);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		for (int i = 0; i < tour.size() - 2; i++) {
//			for (int j = i + 1; j < tour.size(); j++) {
//				if (i == j)
//					continue;
//				System.out.println("((i, j), (i, j)) = ((" + tour.get(i).getFrom() + ", " + tour.get(j).getFrom()
//						+ "), (" + tour.get(j).getFrom() + ", " + tour.get(j).getTo() + "))");
//				swapLocal(tour, i, j);
//				if (getRouteLength(tour, data) < bestDistance) {
//					bestDistance = getRouteLength(tour, data);
//					toBan.add(new Edge(i, j));
//					stacks.clear();
//					response = true;
//				} else {
//					swapLocal(tour, j, i);
//				}
//			}
//		}
//
//		for (int i = 0; i < tour.size(); i++) {
//			for (int j = 0; j < tour.size(); j++) {
//				Edge toPush = new Edge(i, j);
//				if (!toBan.contains(toPush)) {
//					state.add(toPush);
//				}
//			}
//		}
//
//		return response;
//	}
//
//	private static void swapLocal(ArrayList<Edge> tour, int i, int j) {
//		Edge one = new Edge(tour.get(i).getFrom(), tour.get(j).getFrom());
//		Edge other = new Edge(tour.get(i).getTo(), tour.get(j).getTo());
//
//		tour.set(i, one);
//		tour.set(j, other);
//	}

	private static HashMap<String, String> getHashArgs(String[] args) {
		if (args.length % 2 != 0) {
			System.out.println("Wrong number of arguments.");
			System.exit(1);
		}

		HashMap<String, String> hashArgs = new HashMap<String, String>();
		for (int i = 0; i < args.length; i += 2) {
			hashArgs.put(args[i], args[i + 1]);
		}

		return hashArgs;
	}

}
