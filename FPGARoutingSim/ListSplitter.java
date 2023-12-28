/**
 * @author Stefan Reichel
 */
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for splitting a list into partitions of a specified size.
 */
public class ListSplitter {

    /**
     * Splits a given list into partitions of a specified size.
     *
     * @param originalList   The original list to be split.
     * @param partitionSize  The size of each partition.
     * @param <T>            The type of elements in the list.
     * @return A list of partitions, where each partition is a sublist of the original list.
     */
    public static <T> List<List<T>> splitList(List<T> originalList, int partitionSize) {
        List<List<T>> partitions = new ArrayList<>();

        for (int i = 0; i < originalList.size(); i += partitionSize) {
            int end = Math.min(i + partitionSize, originalList.size());
            partitions.add(new ArrayList<>(originalList.subList(i, end)));
        }

        return partitions;
    }
}
