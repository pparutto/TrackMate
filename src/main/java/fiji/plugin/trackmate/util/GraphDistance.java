package fiji.plugin.trackmate.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import ij.ImagePlus;

public class GraphDistance
{
	private static class Node
	{
		public int x;
		public int y;

		public Node(int[] p)
		{
			this.x = p[0];
			this.y = p[1];
		}

		public Node(int i, int j)
		{
			this.x = i;
			this.y = j;
		}

		@Override
		public boolean equals(Object o)
		{
			if (o == this)
				return true;
			if (!(o instanceof Node))
				return false;

			Node n2 = (Node) o;
			return this.x == n2.x && this.y == n2.y;
		}

		@Override
		public int hashCode()
		{
			return this.x * this.y;
		}
	}

	static private Node minDistNode(final HashMap< Node, Double > dist, final HashSet< Node > q)
	{
		double curD = 9999999;
		Node curNode = null;
		for (final Node n: dist.keySet())
		{
			if (dist.get(n) < curD && q.contains(n))
			{
				curD = dist.get(n);
				curNode = n;
			}
		}

		return curNode;
	}

	static private ArrayList< Node > findNeighbors(Node n, final ImagePlus mask)
	{
		ArrayList< Node > res = new ArrayList< >();
		for (int[] nh: new int[][] {new int[] {-1, -1}, new int[] {-1, 0},
									new int[] {-1, 1},  new int[] {0, -1},
									new int[] {0, +1},  new int[] {+1, -1},
									new int[] {+1, 0},  new int[] {+1, +1}})
		{
			int[] p = new int[] {n.x + nh[0], n.y + nh[1]};

			if (p[0] >= 0 && p[1] >= 0 && p[0] < mask.getWidth() &&
				p[1] < mask.getHeight())
				res.add(new Node(p));
		}

		return res;
	}

	static public double dijkstra(final ImagePlus mask, int[] source, int[] dest)
	{
		//Pierre note: here we do not actually need prev
		HashMap< Node, Double > dist = new HashMap< >();
		HashMap< Node, Node > prev = new HashMap< >();

		HashSet<Node> q = new HashSet< >();
		for ( int i = 0; i < mask.getWidth(); ++i )
		{
			for ( int j = 0; j < mask.getHeight(); ++j )
			{
				Node tmp = new Node( i, j );
				q.add( tmp );

				if ( i == source[0] && j == source[1] )
					dist.put(tmp, 0.0);
				else
					dist.put(tmp, 999999.0);
			}
		}

		while ( !q.isEmpty() )
		{
			System.out.println(q.size());
			Node n = minDistNode(dist, q);
			q.remove(n);

			for (Node nh: findNeighbors(n, mask))
			{
				if (!q.contains(nh))
					continue;

				double alt = dist.get(n) + 1;

				if (alt < dist.get(nh))
				{
					dist.put(nh, alt);
					prev.put(nh, n);
				}
			}
		}

		return dist.get(new Node(dest));
	}
}
