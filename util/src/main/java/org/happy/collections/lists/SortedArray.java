package org.happy.collections.lists;

import com.gs.collections.impl.block.factory.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntFunction;

/**
 * {@link SortedList_1x4} is a decorator which decorates {@link List}. Keep in
 * mind that {@link SortedList_1x4} violates the contract of {@link List}
 * interface, because inserted elements don't stay in the inserted order, but
 * will be sorted according to used comparator.
 * <p>
 * {@link SortedList_1x4} supports two types of sorting-strategy:
 * <ul>
 * <li> {@link SearchType#BinarySearch} - uses binary search and is suitable for
 * lists like {@link ArrayList} or {@link TreeList}, where elements can be
 * cheaply accessed by their index.</br> The complexity of {link SortType#Array}
 * is equal to N*logN</li>
 * <li>{@link SearchType#LinearSearch} - uses insertion sort to insert new
 * elements. Insertion sort starts to search for element from the beginning of
 * the list until the index for insertion is found and inserts then the new
 * element. This type of sorting is suitable for {@link LinkedList}. insertion
 * Sort has complexity between N (best-case) and N^2 (worst-case)</li>
 * </ul>
 * <p/>
 * {@link SortedList_1x4} implements all methods provided by
 * {@link NavigableSet}, but requirements of such methods are adopted to the
 * {@link List} interface which can contain duplicates and thus differs from
 * {@link NavigableSet}.
 * <p>
 * <p>
 * Here is a small examle how to store few integers in a {@link SortedList_1x4}
 * :</br></br> final List<Integer> sortedList =
 * Collections_1x2.sortedList();</br> sortedList.addAll(Arrays.asList(new
 * Integer[] { 5, 0, 6, 5, 3, 4 }));</br>
 * System.out.print(sortedList.toString());</br>
 * </p>
 * The output on console is: {@code [0, 3, 4, 5, 5, 6]}
 *
 * @param <E>
 * @author Andreas Hollmann
 */
public final class SortedArray<E> {


    protected final int binarySearchThreshold = 8;
    private final IntFunction<E[]> builder;
    private E[] list;
    private int size;

    public E[] array() {
        return list;
    }

    public int size() {
        return size;
    }

    public E remove(int index) {
        E previous = this.list[index];
        int totalOffset = this.size - index - 1;
        if (totalOffset > 0)
        {
            System.arraycopy(this.list , index + 1, this.list, index, totalOffset);
        }
        this.list[--this.size] = null;
        return previous;
    }

    public boolean remove(E removed) {
        int i = indexOf(removed);
        if (i == -1)
            return false;
        return remove(i)!=null;
    }

    public void clear() {
        final int s = size();
        if (s!=0) {
            Arrays.fill(list, 0, s, null);
            this.size = 0;
        }
    }


    /**
     * defines the sorting strategy for {@link SortedList_1x4}
     *
     * @author Andreas Hollmann
     */
    public enum SearchType {
        /**
         * uses binary search and is suitable for lists like {@link ArrayList}
         * or {@link TreeList}, where elements can be cheaply accessed by their
         * index.</br>
         */
        BinarySearch,
        /**
         * uses insertion sort to insert new elements. Insertion sort starts to
         * search for element from the beginning of the list until the index for
         * insertion is found and inserts then the new element. This type of
         * sorting is suitable for {@link LinkedList}. insertion Sort has
         * complexity between N (best-case) and N^2 (worst-case)
         */
        LinearSearch
    }

    private final Comparator<E> comparator;
    /**
     * the type of the sorting algorithm
     */
    private final SearchType sortType;


    /**
     * constructor
     *
     * @param list       decorated List
     * @param comparator comparator
     * @param searchType type for the sorting strategy, there are two types for the
     *                   LinkedList and for ArrayList with binary search-methods
     * @param inverted   if true the sorted order will be inverted, the list will be
     *                   sorted in descending order
     * @param preSort    if true the the list, which is the parameter in the
     *                   constructor will be sorted, before it will be decorated
     */
    public SortedArray(final IntFunction<E[]> builder, final Comparator<E> comparator,
                       final SearchType searchType, int initialCapacity) {
        //this.setDecoratedInternally(decorated); //Collections_1x4.failFastList(decorated));

        this.builder = builder;
        this.comparator = comparator;
        this.sortType = searchType;
        setCapacity(initialCapacity);
    }

    public void setCapacity(int cap) {
        if (this.list == null || this.list.length != cap) {
            this.list = builder.apply(cap);
        }
    }


    public void forEach(Consumer<? super E> action) {
        for (E x : list)
            action.accept(x);
    }


    public boolean add(final E element) {
        // use the linked-list sorting if the type is set or if the list-size
        // is to small
        int s = this.size;

        Comparator<E> cmp = this.comparator;
        if ((s < binarySearchThreshold) || SearchType.LinearSearch == this.sortType) {
            return addLinear(element, s, cmp);
        } else {
            return addBinary(element, s, cmp);
        }

    }

    public boolean addBinary(E element, int s, Comparator<E> cmp) {
        // use the binary search
        final int index = this.findInsertionIndex_TypeArray(element, 0, s - 1, new int[1]);
        final int sz = s;
        final E last = list[sz - 1];
        if (index == sz || cmp.compare(last, element) < 0) {
            addInternal(element);
        } else {
            if (index == -1)
                addInternal(element);
            else
                addInternal(index, element);
        }
        return true;
    }

    public boolean addLinear(E element, int s, Comparator<E> cmp) {
        for (int i = 0; i < s; i++) {
            final E current = list[i];
            if (0 <= cmp.compare(current, element)) {
                addInternal(i, element);
                return true;
            }
        }
        return addInternal(element); //add to end
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean addInternal(E e) {
        int s = this.size;
        if (this.list.length == s) {
            E[] newList = builder.apply(grow(s));
            System.arraycopy(this.list, 0, newList, 0, s);
            this.list = newList;
		}
        this.list[this.size++] = e;
        return true;
    }

    public void addInternal(int index, E e) {
        if (index > -1 && index < this.size) {
            this.addAtIndex(index, e);
        } else if (index == this.size) {
            this.addInternal(e);
        }
//		else
//		{
//			this.throwOutOfBounds(index);
//		}
    }

    private void addAtIndex(int index, E element) {
        int oldSize = this.size++;
        if (this.list.length == oldSize) {
            E[] newItems = builder.apply(grow(oldSize)); //new Object[this.sizePlusFiftyPercent(oldSize)];
            if (index > 0) {
                System.arraycopy(this.list, 0, newItems, 0, index);
            }
            System.arraycopy(this.list, index, newItems, index + 1, oldSize - index);
            this.list = newItems;
        } else {
            System.arraycopy(this.list, index, this.list, index + 1, oldSize - index);
        }
        this.list[index] = element;
    }

    private int grow(int oldSize) {
        if (oldSize == 0) return 1;
        return (int) Math.ceil(oldSize * 1.5f);
    }

    public E removeLast() {
        return this.list[--size];
    }


//	/**
//	 * this method throws IllegalStateException if the sorted order can be
//	 * violated through the new inserted value
//	 */
//	@Override
//	public void add(final int index, final E element) {
//		if (0 < index) {// proof element before
//			final E prevElem = list.get(index - 1);
//			final int cmp = this.comparator.compare(prevElem, element);// compare
//																		// to
//																		// previews
//																		// object
//			if (0 < cmp) {
//				throw new IllegalStateException(
//						"SortedList_1x0.set(int index, E element) caused exception, because "
//								+ element.toString()
//								+ "is bigger then "
//								+ list.get(index - 1)
//										.toString());
//			}
//		}
//		if (index < list.size() - 2) {// proof element
//																// after
//			final E nextElem = list.get(index + 1);
//			final int cmp = this.comparator.compare(element, nextElem);// compare
//																		// to
//																		// successor
//																		// object
//			if (0 < cmp) {
//				throw new IllegalStateException(
//						"SortedList_1x0.set(int index, E element) caused exception, because "
//								+ element.toString()
//								+ "is smaller then "
//								+ list.get(index + 1)
//										.toString());
//			}
//		}
//		list.add(index, element);
//	}

    /**
     * adds all elements to the decorated list
     */
    public boolean addAll(final Collection<? extends E> c) {
        boolean changed = false;
        for (final E element : c) {
            changed = this.add(element) || changed;
        }
        return changed;
    }

//	public boolean addAll(final int index, final Collection<? extends E> c) {
//
//		if (c.isEmpty()) {
//			return true;
//		}
//		// sort collection before adding
//		final List<E> list = new ArrayList<E>(c);
//		Collections.sort(list, this.comparator);
//		for (final E elem : c) {
//			list.add(elem);
//		}
//
//		// get first and last elements
//		final E firstElem = list.get(0);
//		final E lastElem = list.get(list.size() - 1);
//
//		if (0 < index) {// proof element before
//			final int result = this.comparator.compare(this.list.get(index - 1), firstElem);// compare
//																			// to
//			// previews
//			// object
//			if (result < 0) {
//				throw new IllegalStateException(
//						"SortedList_1x0.set(int index, E element) caused exception, because "
//								+ firstElem.toString()
//								+ "is bigger then "
//								+ this.list.get(index - 1)
//										.toString());
//			}
//		}
//		if (index < this.list.size() - 2) {// proof element
//																// after
//			final int result = this.comparator.compare(lastElem, this.list.get(index + 1));// compare to
//																// successor
//			// object
//			if (0 < result) {
//				throw new IllegalStateException(
//						"SortedList_1x0.set(int index, E element) caused exception, because "
//								+ lastElem.toString()
//								+ "is smaller then "
//								+ this.list.get(index + 1)
//										.toString());
//			}
//		}
//
//		// add sorted collection
//		return this.list.addAll(index, list);
//	}

//	/**
//	 * sets the element to the new value, but this method proof the new input
//	 * and if it destroys the sorted order of the list, this method throws the
//	 * IllegalArgumentException
//	 */
//	@Override
//	public E set(final int index, final E element) {
//
//		if (0 < index) {// proof element before
//			final int result = this.comparator.compare(list.get(index - 1), element);// compare
//																		// to
//			// previews
//			// object
//			if (0 < result) {
//				throw new IllegalArgumentException(
//						"SortedList_1x0.set(int index, E element) caused exception, because "
//								+ element.toString()
//								+ "is bigger then "
//								+ list.get(index - 1)
//										.toString());
//			}
//		}
//		if (index < list.size() - 2) {// proof element
//																// after
//			final int result = this.comparator.compare(element, list.get(index + 1));// compare to
//																// successor
//			// object
//			if (0 < result) {
//				throw new IllegalArgumentException(
//						"SortedList_1x0.set(int index, E element) caused exception, because "
//								+ element.toString()
//								+ "is smaller then "
//								+ list.get(index + 1)
//										.toString());
//			}
//		}
//
//		return list.set(index, element);
//	}


	@SuppressWarnings("unchecked")
	public int indexOf(@NotNull final E element) {

		if (element == null)
			return -1;

		if (SearchType.LinearSearch == this.sortType) {
			return indexOfInternal(element);
		}

		int size = size();
		if (size < binarySearchThreshold) {
			return indexOfInternal(element);
		}


        final int[] rightBorder = new int[] { 0 };
		final int left = this.findInsertionIndex_TypeArray(element, 0, size, rightBorder);

        E[] l = this.list;
		for (int index = left; index < rightBorder[0]; index++) {
            if (element.equals(l[index])) {
				return index;// element is found
			}
		}

		//return -1;
        //worst case, not found because not sorted:
        return indexOfInternal(element);

	}

    private final int indexOfInternal(E e) {
        E[] l = this.list;
        for (int i = 0; i < size; i++) {
            if (l[i].equals(e)) {
                return i;
            }
        }
        return -1;
    }

//	@SuppressWarnings("unchecked")
//	@Override
//	public int lastIndexOf(final Object element) {
//		if (list.size() < binarySearchThreshold
//				|| SearchType.LinearSearch.equals(this.sortType)) {
//			return super.lastIndexOf(element);
//		}
//
//		E elemE = null;
//		try {
//			elemE = (E) element;
//		} catch (final ClassCastException ex) {
//			return -1;
//		}
//		if (elemE == null) {
//			return -1;
//		}
//
//		final int[] rightBorder = new int[] { 0 };
//		final int left = this.findInsertionIndex_TypeArray(elemE, 0, list.size(), rightBorder);
//
//		//TODO convert this to a non-iterator loop
//
//		int index = left;
//		final ListIterator<E> it = list.listIterator(left);
//		boolean found = false;
//		while (it.hasNext()) {
//			final E e = it.next();
//			if (rightBorder[0] < index) {
//				break;
//			}
//
//			if (elemE.equals(e)) {
//				// element is found
//				found = true;
//			} else {
//				if (found) {
//					return index - 1;
//				}
//			}
//			index++;
//		}
//		if (found) {
//			return index - 1;
//		}
//		return -1;
//
//	}

    /**
     * find the position where the object should be inserted in to the list, or
     * the area of the list which should be searched for the object
     *
     * @param list        the list or sublist in where the index should be found
     * @param element     element for which the index should be found
     * @param left        the left index (inclusively)
     * @param right       the right index (exclusively)
     * @param rightBorder This parameter will be modified inside the method, thus you
     *                    can analyse it after the method execution. Is used to know the
     *                    right border.
     * @return first index of the element where the element should be inserted
     */
    private int findInsertionIndex_TypeArray(
            final E element, final int left, final int right,
            @NotNull final int[] rightBorder) {
		/*if (rightBorder == null) {
			throw new IllegalArgumentException("rightBorder can't be null");
		}*/

        if (right < left) {
            throw new IllegalArgumentException(
                    "right must be bigger or equals as the left");
        }

        if ((right - left) <= binarySearchThreshold) {
            rightBorder[0] = right;//.setObject(right);
            return findFirstIndex_TypeLinked(element, left, right);
        }
        final int midle = left + (right - left) / 2;
        final E midleE = list[midle];

        final Comparator<E> cmp = this.comparator;

        final int comparedValue = cmp.compare(midleE, element);
        if (0 < comparedValue) {
            // element < middleE
            return this.findInsertionIndex_TypeArray(element, left, midle, rightBorder);
        } else if (comparedValue == 0) {
            // find the first element
            int index = midle;
            for (; index >= 0; ) {
                final E e = list[index];
                if (0 != cmp.compare(e, element)) {
                    break;
                }
                index--;
            }
            rightBorder[0] = index;
            return index;
        }

        return this.findInsertionIndex_TypeArray(element, midle, right,
                rightBorder);
    }

    /**
     * searches for the first index found for given element
     *
     * @param list    the list or sublist which should be invastigated
     * @param element element for which the index should be found
     * @param left    left index (inclusively)
     * @param right   right index (exclusively)
     * @return
     */
    private int findFirstIndex_TypeLinked(final E element,
                                          final int left, final int right) {

        int index = left;
        final Comparator<E> cmp = this.comparator;
        for (; index < right; ) {
            if (0 <= cmp.compare(list[index], element)) {
                return index;
            }
            index++;
        }
        return right;

//		int index = left;
//		final ListIterator<E> it = list.listIterator(left);
//		while (it.hasNext() && index < right) {
//			final E e = it.next();
//
//			if (0 <= comparator.compare(e, element)) {
//				return index;
//			}
//			index++;
//		}
//		return right;
    }

//	@Override
//	public boolean contains(final Object obj) {
//		return 0 <= this.indexOf(obj);
//	}

//	@Override
//	public boolean remove(final Object obj) {
//		final int index = indexOf(obj);
//		if (0 <= index) {
//			list.remove(index);
//			return true;
//		}
//		return false;
//	}

    /**
     * Returns the comparator used to order the elements in this list
     *
     * @return
     */
    public Comparator<E> comparator() {
        return this.comparator;
    }

    /**
     * Returns the first (lowest) element currently in this list.
     */
    public E first() {
        if (this.isEmpty()) {
            throw new NoSuchElementException();
        } else {
            return list[0];
        }
    }

    /**
     * Returns the last (highest) element currently in this list.
     */
    public E last() {
        int size = this.size;
        if (size == 0) {
            throw new NoSuchElementException();
        } else {
            return this.list[size - 1];
        }
    }

//	@Override
//	public SortedList_1x4<E> subList(final int fromIndex, final int toIndex) {
//		final List<E> list = super.subList(fromIndex, toIndex);
//		final boolean doSort = false;
//		;
//		final SearchType sortType = this.getSortType();
//		return SortedList_1x4.of(list, this.comparator(), sortType, doSort);
//	}

//	/**
//	 * Returns a view of the portion of this list whose elements range from
//	 * fromElement, inclusive, to toElement, exclusive. toElement should be not
//	 * equals to one of elements containing in the list, imported is the compare
//	 * value of toElemnt and elements from the list. If the compare value is
//	 * zero it means the element was found. this list view is backed by the
//	 * subList(int fromIndex, int toindex) and became undefined if the backing
//	 * list (i.e., this list) is structurally modified in any way other than via
//	 * the returned list.
//	 *
//	 * @param fromElement
//	 *            (inclusive) all same elements compared to fromElment will be
//	 *            included in the list view.
//	 * @param toElement
//	 *            (exclusive) all same elements compared to toELemnt will be not
//	 *            included in this list view.
//	 * @return
//	 */
//	public SortedList_1x4<E> subList(final E fromElement, final E toElement) {
//		final boolean fromInclusive = true;
//		final boolean toInclusive = false;
//		return subList(fromElement, fromInclusive, toElement, toInclusive);
//	}
//
//	/**
//	 * Returns a view of the portion of this list whose elements range from
//	 * fromElement, inclusive, to toElement, exclusive. toElement should be not
//	 * equals to one of elements containing in the list, imported is the compare
//	 * value of toElemnt and elements from the list. If the compare value is
//	 * zero it means the element was found. this list view is backed by the
//	 * subList(int fromIndex, int toindex) and became undefined if the backing
//	 * list (i.e., this list) is structurally modified in any way other than via
//	 * the returned list.
//	 *
//	 * @param fromElem
//	 *            (inclusive) all same elements compared to fromElment will be
//	 *            included in the list view.
//	 * @param toElem
//	 *            (exclusive) all same elements compared to toELemnt will be not
//	 *            included in this list view.
//	 * @return
//	 */
//	public SortedList_1x4<E> subList(final E fromElem,
//			final boolean fromInclusive, final E toElem,
//			final boolean toInclusive) {
//		final boolean fromLimmited = true;
//		final boolean toLimmited = true;
//		final List<E> subList = new LimmitedList(fromElem, fromInclusive,
//				fromLimmited, toElem, toInclusive, toLimmited);
//		final boolean doSort = false;
//		final SearchType sortType = getSortType();
//		return SortedList_1x4.of(subList, this.comparator(), sortType, doSort);
//	}
//
//	/**
//	 * gets the list iterator which stops at greatest element in this set
//	 * strictly less than the given element. if all elements in the list are
//	 * greater than given element, then iterator points to the start of the
//	 * list. If all elements are smaller than given element, then iterator
//	 * points to the last element of the list.
//	 *
//	 * @param elem
//	 *            element which should be used to find comparable element in the
//	 *            list
//	 */
//	public ListIterator<E> lowerIterator(final E elem) {
//		final ListIterator<E> it = this.ceilingIterator(elem);
//		final Comparator<E> c = this.comparator();
//		while (it.hasPrevious()) {
//			final E current = it.previous();
//			final int cmp = c.compare(current, elem);
//			if (cmp < 0) {
//				return it;
//			}
//		}
//		return it;
//	}
//
//	/**
//	 * Returns the greatest element in this set strictly less than the given
//	 * element, or {@code null} if there is no such element.
//	 *
//	 * @param e
//	 *            the value to match
//	 * @return the greatest element less than {@code e}, or {@code null} if
//	 *         there is no such element
//	 * @throws ClassCastException
//	 *             if the specified element cannot be compared with the elements
//	 *             currently in the set
//	 * @throws NullPointerException
//	 *             if the specified element is null and this set does not permit
//	 *             null elements
//	 */
//	public E lower(final E e) {
//		final ListIterator<E> it = this.lowerIterator(e);
//		if (it.hasNext()) {
//			final E current = it.next();
//			final Comparator<E> c = this.comparator();
//			final int cmp = c.compare(current, e);
//			if (cmp < 0) {
//				return current;
//			}
//		}
//		return null;
//
//	}
//
//	/**
//	 * gets the list iterator which points at the greatest element in this list
//	 * less than or equal to the given element. if all elements in the list are
//	 * greater than given element, then iterator points to the start of the
//	 * list. If all elements are smaller than given element, then iterator
//	 * points to the last element of the list. </br> if you have a list with
//	 * elements (a,b1,b2,c) where b1.equals(b2)=false, but
//	 * comparator.compare(b1,b2)==0. Then call of method floorIterator(b1) would
//	 * return iterator which points to b2. also if you have another element b3
//	 * and c.compare(b2,b3)==0 then the call of method floor(b3) returns b2.
//	 *
//	 * @param element
//	 *            element which should be used to find comparable element in the
//	 *            list
//	 */
//	public ListIterator<E> floorIterator(final E element) {
//		final ListIterator<E> it = this.ceilingIterator(element);
//		final Comparator<E> c = this.comparator();
//		while (it.hasNext()) {
//			final E current = it.next();
//			final int cmp = c.compare(current, element);
//			if (0 < cmp) {
//				break;
//			}
//		}
//		while (it.hasPrevious()) {
//			final E current = it.previous();
//			final int cmp = c.compare(current, element);
//			if (cmp <= 0) {
//				return it;
//			}
//		}
//		return it;
//	}
//
//	/**
//	 * Returns the greatest element in this list less than or equal to the given
//	 * element, or {@code null} if there is no such element. if you have a list
//	 * with elements (a,b1,b2,c) where at b1.equals(b2)=false, but
//	 * comparator.compare(b1,b2)==0. Then call of method floor(b1) would return
//	 * b2. also if you have another element b3 and c.compare(b1,b3)==0 then the
//	 * call of method floor(b3) returns b2.
//	 *
//	 * @param e
//	 *            the value to match
//	 * @return the greatest element less than or equal to {@code e}, or
//	 *         {@code null} if there is no such element
//	 * @throws ClassCastException
//	 *             if the specified element cannot be compared with the elements
//	 *             currently in the set
//	 * @throws NullPointerException
//	 *             if the specified element is null and this set does not permit
//	 *             null elements
//	 */
//	public E floor(final E e) {
//		if (this.isEmpty()) {
//			return null;
//		}
//		final ListIterator<E> it = this.floorIterator(e);
//		if (!it.hasPrevious()) {
//			final E current = it.next();
//			final int cmp = this.comparator().compare(current, e);
//			if (cmp <= 0) {
//				return current;
//			} else {
//				return null;
//			}
//		} else {
//			return it.next();
//		}
//	}
//
//	/**
//	 * gets the list iterator which points to greater than or equal to the given
//	 * element. If all elements of the list are smaller than given element, then
//	 * list-iterator points to the end of the list if all elements of the list
//	 * are greater than given element, then list-iterator points to the first
//	 * element in the list. </br> if you have a list with elements (a,b,c1,c2,d)
//	 * where at c1.equals(c2)=false, but comparator.compare(c1,c2)==0. Then call
//	 * of method ceilingIterator(b) would return iterator which points to c1.
//	 * The call of method ceilingIterator(c1) returns iterator which points to
//	 * c2.
//	 *
//	 * @param elem
//	 *            given element
//	 */
//	public ListIterator<E> ceilingIterator(final E elem) {
//		Preconditions.checkNotNull(elem);
//		int index = -1;
//		final List<E> list = this.list;
//		final int left = 0;
//		final int right = this.list.size();
//		switch (this.sortType) {
//		case BinarySearch:
//			index = this.findInsertionIndex_TypeArray(elem, left, right, new int[1]);
//			break;
//
//		case LinearSearch:
//			index = this.findFirstIndex_TypeLinked(list, elem, left, right);
//			break;
//		default:
//			throw new IllegalStateException("Not supported type! "
//					+ this.sortType);
//		}
//		if (index == 0) {
//			return this.listIterator();
//		} else if (this.size() <= index) {
//			return this.listIterator(this.size());// list iterator to the last
//													// element
//		} else {
//			final ListIterator<E> it = this.listIterator(index);
//			int cmpLast = -1;// the compare value for previous element
//			while (it.hasNext()) {
//				final E current = it.next();
//				final int cmp = this.comparator.compare(current, elem);
//				if (0 < cmp) {
//					it.previous();
//					if (cmpLast == 0 && it.hasPrevious()) {
//						it.previous();
//					}
//					break;
//				}
//				cmpLast = cmp;
//			}
//			if (!it.hasNext() && cmpLast == 0) {
//				it.previous();
//			}
//			return it;
//		}
//	}
//
//	/**
//	 * Returns the least element in this list greater than or equal to the given
//	 * element, or {@code null} if there is no such element. if you have a list
//	 * with elements (a,b,c1,c2,d) where at c1.equals(c2)=false, but
//	 * comparator.compare(c1,c2)==0. Then call of method ceiling(b) would return
//	 * c1. The call of method ceiling(c1) returns c2.
//	 *
//	 * @param e
//	 *            the value to match
//	 * @return the least element greater than or equal to {@code e}, or
//	 *         {@code null} if there is no such element
//	 * @throws ClassCastException
//	 *             if the specified element cannot be compared with the elements
//	 *             currently in the set
//	 * @throws NullPointerException
//	 *             if the specified element is null and this set does not permit
//	 *             null elements
//	 */
//	public E ceiling(final E e) {
//		final ListIterator<E> it = this.ceilingIterator(e);
//		if (!it.hasNext()) {
//			return null;
//		} else {
//			return it.next();
//		}
//	}
//
//	/**
//	 * gets the list iterator which points to the least element in this set
//	 * strictly greater than the given element. If all elements of the list are
//	 * smaller than given element, then list-iterator points to the end of the
//	 * list and has not nextElement if all elements of the list are greater than
//	 * given element, then list-iterator points to the first element in the
//	 * list.
//	 *
//	 * @param elem
//	 *            given element
//	 */
//	public ListIterator<E> higherIterator(final E elem) {
//		final ListIterator<E> it = ceilingIterator(elem);
//
//		final Comparator<E> c = this.comparator();
//		int cmp = 0;
//		while (it.hasNext()) {
//			final E current = it.next();
//			cmp = c.compare(current, elem);
//			if (cmp != 0) {
//				Preconditions
//						.checkState(0 < cmp,
//								"current element should be greater then elem at this point");
//				it.previous();// go back to the element which was already higher
//								// then the elem
//				break;
//			}
//		}
//		return it;
//	}
//
//	/**
//	 * Returns the least element in this set strictly greater than the given
//	 * element, or {@code null} if there is no such element.
//	 *
//	 * @param e
//	 *            the value to match
//	 * @return the least element greater than {@code e}, or {@code null} if
//	 *         there is no such element
//	 * @throws ClassCastException
//	 *             if the specified element cannot be compared with the elements
//	 *             currently in the set
//	 * @throws NullPointerException
//	 *             if the specified element is null and this set does not permit
//	 *             null elements
//	 */
//	public E higher(final E e) {
//		final ListIterator<E> it = this.higherIterator(e);
//		if (!it.hasNext()) {
//			return null;
//		} else {
//			return it.next();
//		}
//	}
//
//	/**
//	 * Retrieves and removes the first (lowest) element, or returns {@code null}
//	 * if this list is empty.
//	 *
//	 * @return the first element, or {@code null} if this set is empty
//	 */
//	public E pollFirst() {
//		if (this.isEmpty()) {
//			return null;
//		}
//		final ListIterator<E> it = this.listIterator();
//		final E firstElem = it.next();
//		it.remove();
//		return firstElem;
//	}
//
//	/**
//	 * Retrieves and removes the last (highest) element, or returns {@code null}
//	 * if this set is empty.
//	 *
//	 * @return the last element, or {@code null} if this set is empty
//	 */
//	public E pollLast() {
//		if (this.isEmpty()) {
//			return null;
//		}
//		final ListIterator<E> it = this.descendingIterator();
//		final E lastElem = it.next();
//		it.remove();
//		return lastElem;
//	}
//
////	/**
////	 * Returns a reverse order view of the elements contained in this list. The
////	 * descending list is backed by this list, so changes to the list are
////	 * reflected in the descending list, and vice-versa. If either list is
////	 * modified while an iteration over either set is in progress (except
////	 * through the iterator's own {@code remove} operation), the results of the
////	 * iteration are undefined.
////	 *
////	 * <p>
////	 * The returned set has an ordering equivalent to
////	 * <tt>{@link Collections#reverseOrder(Comparator) Collections.reverseOrder}(comparator())</tt>
////	 * . The expression {@code list.descendingList().descendingList()} returns a
////	 * view of {@code s} essentially equivalent to {@code s}.
////	 *
////	 * @return a reverse order view of this set
////	 */
////	public SortedList_1x4<E> descendingList() {
////		final Comparator<E> inversedComparator = Comparators_1x4
////				.inverseComparator(this.comparator());
////		final List<E> reveresedList = Lists.reverse(this);
////		final boolean doSort = false;
////		final SearchType type2 = this.getSortType();
////		return SortedList_1x4.of(reveresedList, inversedComparator, type2,
////				doSort);
////	}
//
//	/**
//	 * Returns an iterator over the elements in this list, in descending order.
//	 * Equivalent in effect to {@code descendingList().iterator()}.
//	 *
//	 * @return an iterator over the elements in this set, in descending order
//	 */
//	public ListIterator<E> descendingIterator() {
//		throw  new UnsupportedOperationException();
////		final ListIterator<E> it = this.listIterator(this.size());
////		return InversedListIterator_1x0.of(it);
//	}
//
//	/**
//	 * Returns a view of the portion of this list whose elements are strictly
//	 * less than toElement.
//	 */
//	public SortedList_1x4<E> headList(final E toElement) {
//		return this.headList(toElement, false);
//	}
//
//	/**
//	 * Returns a view of the portion of this list whose elements are less than
//	 * (or equal to, if {@code inclusive} is true) {@code toElement}. The
//	 * returned list is backed by this list, so changes in the returned list are
//	 * reflected in this list, and vice-versa. The returned list supports all
//	 * optional list operations that this list supports.
//	 *
//	 * <p>
//	 * The returned list will throw an {@code IllegalArgumentException} on an
//	 * attempt to insert an element outside its range.
//	 *
//	 * @param toElement
//	 *            high endpoint of the returned list
//	 * @param inclusive
//	 *            {@code true} if the high endpoint is to be included in the
//	 *            returned view
//	 * @return a view of the portion of this list whose elements are less than
//	 *         (or equal to, if {@code inclusive} is true) {@code toElement}
//	 * @throws ClassCastException
//	 *             if {@code toElement} is not compatible with this lists's
//	 *             comparator (or, if the list has no comparator, if
//	 *             {@code toElement} does not implement {@link Comparable}).
//	 *             Implementations may, but are not required to, throw this
//	 *             exception if {@code toElement} cannot be compared to elements
//	 *             currently in the list.
//	 * @throws NullPointerException
//	 *             if {@code toElement} is null and this list does not permit
//	 *             null elements
//	 * @throws IllegalArgumentException
//	 *             if this list itself has a restricted range, and
//	 *             {@code toElement} lies outside the bounds of the range
//	 */
//	public SortedList_1x4<E> headList(final E toElement, final boolean inclusive) {
//		final E fromElem = null;
//		final boolean fromInclusive = true;
//		final boolean toInclusive = inclusive;
//		final boolean fromLimmited = false;
//		final boolean toLimmited = true;
//		final List<E> headList = new LimmitedList(fromElem, fromInclusive,
//				fromLimmited, toElement, toInclusive, toLimmited);
//
//		final boolean doSort = false;
//		final SearchType sortType = this.getSortType();
//		return SortedList_1x4.of(headList, this.comparator(), sortType, doSort);
//	}
//
//	/**
//	 * Returns a view of the portion of this list whose elements are greater
//	 * than or equal compared to fromElement. If the compare value is zero then
//	 * values are the equal.
//	 */
//	public SortedList_1x4<E> tailList(final E fromElement) {
//		final boolean inclusive = true;
//		return this.tailList(fromElement, inclusive);
//	}
//
//	/**
//	 * Returns a view of the portion of this list whose elements are greater
//	 * than (or equal to, if {@code inclusive} is true) {@code fromElement}. The
//	 * returned list is backed by this list, so changes in the returned list are
//	 * reflected in this list, and vice-versa. The returned list supports all
//	 * optional list operations that this list supports.
//	 *
//	 * <p>
//	 * The returned list will throw an {@code IllegalArgumentException} on an
//	 * attempt to insert an element outside its range.
//	 *
//	 * @param fromElement
//	 *            low endpoint of the returned list
//	 * @param inclusive
//	 *            {@code true} if the low endpoint is to be included in the
//	 *            returned view
//	 * @return a view of the portion of this list whose elements are greater
//	 *         than or equal to {@code fromElement}
//	 * @throws ClassCastException
//	 *             if {@code fromElement} is not compatible with this set's
//	 *             comparator (or, if the set has no comparator, if
//	 *             {@code fromElement} does not implement {@link Comparable}).
//	 *             Implementations may, but are not required to, throw this
//	 *             exception if {@code fromElement} cannot be compared to
//	 *             elements currently in the list.
//	 * @throws NullPointerException
//	 *             if {@code fromElement} is null and this list does not permit
//	 *             null elements
//	 * @throws IllegalArgumentException
//	 *             if this list itself has a restricted range, and
//	 *             {@code fromElement} lies outside the bounds of the range
//	 */
//	public SortedList_1x4<E> tailList(final E fromElement,
//			final boolean inclusive) {
//		final boolean fromInclusive = inclusive;
//		final E toElement = null;
//		final boolean toInclusive = false;
//		final boolean fromLimmited = true;
//		final boolean toLimmited = false;
//		final List<E> tailList = new LimmitedList(fromElement, fromInclusive,
//				fromLimmited, toElement, toInclusive, toLimmited);
//		final boolean doSort = false;
//		final SearchType sortType = this.getSortType();
//		return SortedList_1x4.of(tailList, this.comparator(), sortType, doSort);
//	}
//
//	/**
//	 * gets the type of sorting algorithm which is used to sort elements
//	 */
//	public SearchType getSortType() {
//		return sortType;
//	}
//
//	/**
//	 * this class is used to provide a view of this list between fromElement and
//	 * toElement
//	 *
//	 * @author Andreas Hollmann
//	 *
//	 */
//	private class LimmitedList extends AbstractList<E> {
//		private final E fromElem;
//		private final boolean fromInclusive;
//		private final boolean fromLimmited;
//		private final E toElem;
//		private final boolean toInclusive;
//		private final boolean toLimmited;
//
//		/**
//		 * constructor
//		 *
//		 * @param fromElem
//		 *            if {@code null} then the list is not limited by lower
//		 *            limit
//		 * @param fromInclusive
//		 *            if true then fromElem is included in the view
//		 * @param fromlimmited
//		 *            if true then fromElem is as lower bound
//		 * @param toElem
//		 *            if {@code null} then the list is not limited by upper
//		 *            limit
//		 * @param toInclusive
//		 *            if true then toElem is included in the view
//		 * @param tolimmited
//		 *            if true then toElem is as upper bound
//		 */
//		private LimmitedList(final E fromElem, final boolean fromInclusive,
//				final boolean fromLimmited, final E toElem,
//				final boolean toInclusive, final boolean toLimmited) {
//			super();
//
//			if (fromLimmited & toLimmited) {
//				final int cmp = comparator().compare(fromElem, toElem);
//				Preconditions.checkArgument(cmp <= 0,
//						"fromElement is greater then toElement: fromElement="
//								+ fromElem + " toElement=" + toElem);
//			}
//			this.fromElem = fromElem;
//			this.fromInclusive = fromInclusive;
//			this.toElem = toElem;
//			this.fromLimmited = fromLimmited;
//			this.toInclusive = toInclusive;
//			this.toLimmited = toLimmited;
//		}
//
//		/**
//		 * validates element to not violate the defined boundaries
//		 *
//		 * @param elem
//		 * @return
//		 */
//		private boolean isInsideBoundries(final E elem) {
//			final Comparator<E> c = SortedList_1x4.this.comparator();
//			if (this.fromLimmited) {
//				final int cmp = c.compare(elem, this.fromElem);
//				if (cmp < 0 || (!fromInclusive && cmp == 0)) {
//					// 1) element is smaller as fromElement
//					// 2) element is equals to fromElement but it is exclusively
//					return false;
//				}
//			}
//			if (this.toLimmited) {
//				final int cmp = c.compare(elem, toElem);
//				if (0 < cmp || (!toInclusive && cmp == 0)) {
//					// 1) element is greeter than toElement
//					// 2) element is equals to toElement but it is exclusively
//					return false;
//				}
//			}
//			return true;
//		}
//
//		@Override
//		public E get(final int index) {
//			if (index < 0) {
//				throw new IndexOutOfBoundsException("index can't be negative!");
//			}
//			// find start-index
//			final int startIndex = getStartIndex();
//
//			final int currentIndex = startIndex + index;
//			final E elem = SortedList_1x4.this.get(currentIndex);
//			if (!isInsideBoundries(elem)) {
//				throw new IndexOutOfBoundsException(
//						"index="
//								+ index
//								+ " accesses element "
//								+ elem
//								+ ", which can't be accessed by defined limmits: fromElem="
//								+ fromElem + " toElem=" + toElem);
//			}
//			return elem;
//		}
//
//		/**
//		 * gets the started index, this index should be used relatively to used
//		 * parent SortedList
//		 *
//		 * @return
//		 */
//		private int getStartIndex() {
//			int startIndex = 0;
//			if (this.fromLimmited) {
//				final ListIterator<E> it;
//				if (fromInclusive) {
//					it = SortedList_1x4.this.lowerIterator(this.fromElem);
//					final E currentElem = it.next();
//					if (isInsideBoundries(currentElem)) {
//						it.previous();
//					}
//				} else {
//					it = SortedList_1x4.this.higherIterator(this.fromElem);
//				}
//				startIndex = it.nextIndex();
//				Preconditions.checkState(0 <= startIndex
//						&& startIndex <= SortedList_1x4.this.size());
//			}
//			return startIndex;
//		}
//
//		/**
//		 * gets the end index, this index should be used relatively to used
//		 * parent SortedList
//		 *
//		 * @return
//		 */
//		private int getEndIndex() {
//			int endIndex = SortedList_1x4.this.size();
//			if (endIndex == 0) {
//				return 0;
//			}
//			if (this.toLimmited) {
//				final ListIterator<E> it;
//				if (toInclusive) {
//					it = SortedList_1x4.this.floorIterator(this.toElem);
//				} else {
//					it = SortedList_1x4.this.lowerIterator(this.toElem);
//				}
//				endIndex = it.nextIndex();
//				if (0 < endIndex) {
//					return endIndex + 1;
//				} else if (0 == endIndex) {
//					final E nextElem = it.next();
//					if (isInsideBoundries(nextElem)) {
//						endIndex++;
//					}
//				} else {
//					throw new IllegalStateException(
//							"end Index can't be negative at this state");
//				}
//			}
//			Preconditions.checkState(0 <= endIndex
//					&& endIndex <= SortedList_1x4.this.size());
//			return endIndex;
//		}
//
//		@Override
//		public int size() {
//			final int startIndex = this.getStartIndex();
//			final int endIndex = this.getEndIndex();
//			int size = endIndex - startIndex;
//			if (size < 0) {
//				size = 0;
//			}
//			return size;
//		}
//
//		@Override
//		public E set(final int index, final E element) {
//			if (index < 0) {
//				throw new IndexOutOfBoundsException(
//						"index can't be negative but was index=" + index);
//			}
//			if (!this.isInsideBoundries(element)) {
//				throw new IllegalArgumentException("the element=" + element
//						+ " lies outside of defined limits. fromElem="
//						+ this.fromElem + " toElem=" + toElem);
//			}
//			final int startindex = this.getStartIndex();
//			final int currentIndex = startindex + index;
//			return SortedList_1x4.this.set(currentIndex, element);
//		}
//
//		@Override
//		public void add(final int index, final E element) {
//			if (index < 0) {
//				throw new IndexOutOfBoundsException(
//						"index can't be negative but was index=" + index);
//			}
//			if (!this.isInsideBoundries(element)) {
//				throw new IllegalArgumentException("the element=" + element
//						+ " lies outside of defined limits. fromElem="
//						+ this.fromElem + " toElem=" + toElem);
//			}
//			final int startindex = this.getStartIndex();
//			final int currentIndex = startindex + index;
//			SortedList_1x4.this.add(currentIndex, element);
//		}
//
//		@Override
//		public E remove(final int index) {
//			if (index < 0) {
//				throw new IndexOutOfBoundsException(
//						"index can't be negative but was index=" + index);
//			}
//			final int startindex = this.getStartIndex();
//			final int currentIndex = startindex + index;
//			return SortedList_1x4.this.remove(currentIndex);
//		}
//
//	}
//
//
//	@Override
//	public void clear() {
//		list.clear();
//	}
//
//	@Override
//	public boolean containsAll(final Collection<?> arg0) {
//		return list.containsAll(arg0);
//	}
//
//	@Override
//	public final E get(final int arg0) {
//		return list.get(arg0);
//	}
//
//	@Override
//	public final boolean isEmpty() {
//		return list.isEmpty();
//	}
//
//	@Override
//	public E remove(final int index) {
//		return list.remove(index);
//	}
//
//	@Override
//	public boolean removeAll(final Collection<?> c) {
//		return list.removeAll(c);
//	}
//
//	@Override
//	public boolean retainAll(final Collection<?> c) {
//		return list.retainAll(c);
//	}
//
//	@Override
//	public int size() {
//		return list.size();
//	}
//
//	@Override
//	public Object[] toArray() {
//		return list.toArray();
//	}
//
//	@Override
//	public <T> T[] toArray(final T[] a) {
//		return list.toArray(a);
//	}
//
//	@Override
//	public String toString() {
//		return list.toString();
//	}
//
//	@Override
//	public boolean equals(final Object obj) {
//		return list.equals(obj);
//	}
}
