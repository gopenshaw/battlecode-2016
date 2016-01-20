package nels;

public class DestroyedDenData {
    public int numberOfDens;
    public int[] denId;

    public DestroyedDenData(int numberOfDens) {
        this.numberOfDens = numberOfDens;
        denId = new int[numberOfDens];
    }

    @Override
    public String toString() {
        String s = numberOfDens + ":";
        for (int i = 0; i < numberOfDens; i++) {
            s += " " + denId[i];
        }

        return s;
    }
}
