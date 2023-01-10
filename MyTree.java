import java.util.Comparator;

public class MyTree {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}

class AvlTree<E extends Comparable<? super E>> {
	private AvlNode<E> root;
	
	public AvlTree() {
		root = null;
	}
	
	public void insert(E x) {
		root = insert(x, root);
	}
	
	private static class AvlNode<E> {
		E element;
		AvlNode<E> left;
		AvlNode<E> right;
		int height;
		
		AvlNode(E theElement) {
			this(theElement, null, null);
		}
		
		AvlNode(E theElement, AvlNode<E> lt, AvlNode<E> rt) {
			element = theElement;
			left = lt;
			right = rt;
			height = 0;
		}
	}
	
	private int height(AvlNode<E> t) {
		return t == null ? -1 : t.height;
	}
	
	private AvlNode<E> insert(E x, AvlNode<E> t) {
		if (t == null) 
			return new AvlNode<E>(x, null, null);
		
		int compareResult = x.compareTo(t.element);
		
		if (compareResult < 0)
			t.left = insert(x, t.left);
		else if (compareResult > 0)
			t.right = insert(x, t.right);
		else;
		
		return balance(t);
	}
	
	private static final int ALLOWED_IMBALANCE = 1;
	
	private AvlNode<E> balance(AvlNode<E> t) {
		if (t == null)
			return t;
		
		if (height(t.left) - height(t.right) > ALLOWED_IMBALANCE) {
			if (height(t.left.left) >= height(t.left.right))
				t = rotateWithLeftChild(t);
			else
				t = doubleWithLeftChild(t);
		}
		else if (height(t.right) - height(t.left) > ALLOWED_IMBALANCE) {
			if (height(t.right.right) >= height(t.right.left))
				t = rotateWithRightChild(t);
			else
				t = doubleWithRightChild(t);
		}
		
		t.hight = Math.max(height(t.left), height(t.right)) + 1;
		return t;
	}
	
	private AvlNode<E> rotateWithLeftChild(AvlNode<E> k2) {
		AvlNode<E> k1 = k2.left;
		k2.left = k1.right;
		k1.right = k2;
		k2.height = Math.max(height(k2.left), height(k2.right)) + 1;
		k1.height = Math.max(height(k1.left), k2.height) + 1;
		return k1;
	}
	private AvlNode<E> doubleWithLeftChild(AvlNode<E> k3) {
		k3.left = rotateWithRightChild(k3.left);
		return rotateWithLeftChild(k3);
	}
	private AvlNode<E> rotateWithRightChild(AvlNode<E> k1) {
		AvlNode<E> k2 = k1.right;
		k1.right = k2.left;
		k2.left = k1;
		k1.height = Math.max(height(k1.left), height(k1.right)) + 1;
		k2.height = Math.max(height(k2.right), k1.height) + 1;
		return k2;
	}
	private AvlNode<E> doubleWithRightChild(AvlNode<E> k1) {
		k1.right = rotateWithLeftChild(k1.right);
		return rotateWithRightChild(k1);
	}
	private AvlNode<E> remove(E x, AvlNode<E> t) {
		if (t == null)
			return t;
		
		int compareResult = x.compareTo(t.element);
		
		if (compareResult < 0)
			t.left = remove(x, t.left);
		else if (compareResult > 0)
			t.right = remove(x, t.right);
		else if (t.left != null && t.right != null) {
			t.element = findMin(t.right).element;
			t.right = remove(t.element, t.right);
		}
		else
			t = (t.left != null) ? t.left : t.right;
		
		return balance(t);
	}
	private AvlNode<E> findMin(AvlNode<E> t) {
		if (t == null)
			return t;
		
		if (t.left == null)
			return t;
		else
			return findMin(t.left);
	}
}

class BinarySearchTree<E> {
	private BinaryNode<E> root;
	private Comparator<? super E> cmp;
	
	private static class BinaryNode<E> {
		E element;
		BinaryNode<E> left;
		BinaryNode<E> right;
		
		BinaryNode(E theElement) {
			this(theElement, null, null);
		}
		
		BinaryNode(E theElement, BinaryNode<E> lt, BinaryNode<E> rt) {
			element = theElement;
			left = lt;
			right = rt;
		}
	}
	
	public BinarySearchTree() {
		this(null);
	}
	public BinarySearchTree(Comparator<? super E> c) {
		root = null;
		cmp = c;
	}
	
	public void makeEmpty() {
		root = null;
	}
	public boolean isEmpty() {
		return root == null;
	}
	
	public boolean contains(E x) {
		return contains(x, root);
	}
	public E findMin() {
		if (isEmpty())
			throw new UnderflowException();
		return findMin(root).element;
	}
	public E findMax() {
		if (isEmpty())
			throw new UnderflowExcetion();
		return findMax(root).element;
	}
	
	public void insert(E x) {
		root = insert(x, root);
	}
	public void remove(E x) {
		root = remove(x, root);
	}
	
	public void printTree() {
		if (isEmpty())
			System.out.println("Empty tree");
		else
			printTree(root);
	}
	
	private int myCompare(E lhs, E rhs) {
		if (cmp != null)
			return cmp.compare(lhs, rhs);
		else
			return ((Comparable)lhs).compareTo(rhs);
	}
	private boolean contains(E x, BinaryNode<E> t) {
		if (t == null)
			return false;
		
		int compareResult = myCompare(x, t.element);
		
		if (compareResult < 0)
			return contains(x, t.left);
		else if (compareResult > 0)
			return contains(x, t.right);
		else
			return true;
	}
	private BinaryNode<E> findMin(BinaryNode<E> t) {
		if (t == null)
			return null;
		else if (t.left == null)
			return t;
		return findMin(t.left);
	}
	private BinaryNode<E> findMax(BinaryNode<E> t) {
		if (t != null)
			while (t.right != null)
				t = t.right;
		
		return t;
	}
	
	private BinaryNode<E> insert(E x, BinaryNode<E> t) {
		if (t == null)
			return new BinaryNode<>(x, null, null);
		
		int compareResult = myCompare(x, t.element);
		if (compareResult < 0)
			t.left = insert(x, t.left);
		else if (compareResult > 0)
			t.right = insert(x, t.right);
		else
			;
		
		return t;
	}
	private BinaryNode<E> remove(E x, BinaryNode<E> t) {
		if (t == null)
			return t;
		int compareResult = ((Comparable)x).compareTo(t.element);
		
		if (compareResult < 0)
			t.left = remove(x, t.left);
		else if (compareResult > 0)
			t.right = remove(x, t.right);
		else if (t.left != null && t.right != null) {
			t.element = findMin(t.right).element;
			t.right = remove(t.element, t.right);
		}
		else
			t = (t.left != null) ? t.left : t.right;
		
		return t;
	}
	
	private void printTree(BinaryNode<E> t) {
		if (t != null) {
			printTree(t.left);
			System.out.println(t.element);
			printTree(t.right);
		}
	}
	
	public static <E> int countNodeNumber(BinarySearchTree<E> tree) {
		return countNodeNumber(tree.root);
	}
	private static <E> int countNodeNumber(BinaryNode<E> t) {
		int count = 0;
		
		if (t == null)
			count = 0;
		else if (t.left == null && t.right == null)
			count = 1;
		else 
			count = countNodeNumber(t.left) + countNodeNumber(t.right) + 1;
		
		return count;
	}
	public static <E> int countLeafNumber(BinarySearchTree<E> tree) {
		return countLeafNumber(tree.root);
	}
	private static <E> int countLeafNumber(BinaryNode<E> t) {
		int count = 0;
		
		if (t == null)
			count = 0;
		else if (t.left == null && t.right == null)
			count = 1;
		else
			count = countLeafNumber(t.left) + countLeafNumber(t.right);
		
		return count;
	}
	
	public static <E extends Comparable<? super E>> boolean checkSearchTreeValidity(BinarySearchTree<E> tree) {
		return checkSearchTreeValidity(tree.root);
	}
	private static <E extends Comparable<? super E>> boolean checkSearchTreeValidity(BinaryNode<E> t) {
		if (t == null || t.left == null && t.right == null)
			return true;
		
		if (checkSearchTreeValidity(t.left) && checkSearchTreeValidity(t.right)) {
			int compareResult1 = -1;
			if (t.left != null)
				compareResult1 = getRightmostNode(t.left).element.compareTo(t.element);
			int compareResult2 = 1;
			if (t.right != null)
				compareResult2 = getLeftmostNode(t.right).element.compareTo(t.element);
			
			if (compareResult1 < 0 && compareResult2 > 0)
				return true;
		}
		
		return false;
	}
	private static <E> BinaryNode<E> getRightmostNode(BinaryNode<E> t) {
		if (t.right == null)
			return t;
		else
			return getRightmostNode(t.right);
	}
	private static <E> BinaryNode<E> getLeftmostNode(BinaryNode<E> t) {
		if (t.left == null)
			return t;
		else
			return getLeftmostNode(t.left);
	}
	
	public void deleteLeaves() {
		root = deleteLeaves(root);
	}
	private BinaryNode<E> deleteLeaves(BinaryNode<E> t) {
		if (t == null)
			return t;
		
		if (t.left != null && t.left.left == null && t.left.right == null)
			t.left = null;
		else
			t.left = deleteLeaves(t.left);
		
		if (t.right != null && t.right.left == null && t.right.right == null)
			t.right = null;
		else
			t.right = deleteLeaves(t.right);
		
		return t;
	}
	
	public static <E extends Comparable<? super E>> void printElements(BinarySearchTree<E> tree, E k1, E k2) {
		printElements(tree.root, k1, k2);
	}
	private static <E extends Comparable<? super E>> void printElements(BinaryNode<E> t, E k1, E k2) {
		if (t == null)
			return ;
		
		if (t.element.compareTo(k2) > 0)
			printElements(t.left, k1, k2);
		else if (t.element.compareTo(k2) <= 0 && t.element.compareTo(k1) >= 0) {
			printElements(t.left, k1, k2);
			System.out.print(t.element + " ");
			printElements(t.right, k1, k2);
		}
		else
			printElements(t.right, k1, k2);
	}
}