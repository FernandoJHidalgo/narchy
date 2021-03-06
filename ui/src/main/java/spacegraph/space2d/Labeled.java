package spacegraph.space2d;

import org.jetbrains.annotations.Nullable;

public interface Labeled {

    Surface label();

    @Nullable
    default Runnable labelClicked() {
        return null;
    }

}
