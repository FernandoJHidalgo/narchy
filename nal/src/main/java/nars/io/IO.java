package nars.io;


import com.google.common.io.ByteArrayDataOutput;
import jcog.data.byt.DynBytes;
import jcog.data.byt.RecycledDynBytes;
import jcog.pri.Prioritized;
import nars.Op;
import nars.Task;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.atom.Atomic;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.function.Consumer;

import static nars.Op.COMMAND;

/**
 * Created by me on 5/29/16.
 * <p>
 * a target's op is only worth encoding in more than a byte since there arent more than 10..20 base operator types. that leaves plenty else for other kinds of control codes that btw probably can fit within the (invisible) ascii control codes. i use basically 1 byte for the op, then depending on this if the target is atomic or compound, the atom decoding reads UTF-8 byte[] directly as the atom's key. compound ops are followed by a byte for # of subterms that will follow, forming its subterm vector. except negation, where we can assume it is only arity=1, so its following arity byte is omitted for efficiency.
 * <p>
 * this limits max # of direct subterms to 127 in signed decimals which i currently dont have any problem with. i count volume with short so the total recursive size of a compound limited to ~16000. these limits are more or less arbitrary and can be re-decided.
 * <p>
 * the set of "anom" atoms, and normalized variables are special cases of atoms which get a more compact encoding due to their frequent repeated appearance during internal target activity: one 16-bit containing the 8-bit op select followed by an 8-bit ordinal id up to 127/255 also mostly wasted. this also supporting fast reads/decodes/deserialization that only needs to lookup a particular array index of for the associated globally-shared immutable instance.
 * https:
 * <p>
 * certain subterm implementations have accelerated read/write procedures, such as https:
 * <p>
 * other atoms serialize their entirity as UTF8 byte[], allowing unicode, etc. append() methods write directly to printable output streams or string builders. unnormalized variables, and other special cases get a default UTF-8 encoding for output that is reversible through the general narsese parser as a final option before failing due to suspected garbage input.
 * <p>
 * image / \ markers are represented internally as special un-unifiable vardep's taking the upper 2 indices of the vardep allocations.
 * <p>
 * Int (32-bit integer) subterm type is processed similar to the Anon's and serializes as its default 4-byte low endian ? encoding. special int packing (zig zag etc) could be used to reduce these on a per-instance level for common values like 0, -1, +1, and numbers less than 127. the Int exists for fast arithmetic ops that otherwise would involve an Atom encoding and decoding to an array, and the need to parse the content to determine if 'isInt()' etc.
 * <p>
 * 3 special Bool constants holds results of tautological truths which occur mostly in intermediate target construction/reduction steps, signaling either target construction failure (Null), True (inert when appearing in Conjunction, or simplify Implications to a subj or predicate, etc..), and False which is effectively (--,True). ie. (X && --X) => False, but --(X && --X) => True.
 * <p>
 * the compressibility of individual terms and task byte[] keys is not as as good as a batch block encoding of several terms and tasks sharing repeated common subterms. but an individual byte[] target or task 'key' is still somewhat compressible and snappy and lz4 can produce canonically compressed versions of terms but use of this can be decided depending if the uncompressed string exceeds some global threshold length due to seek acceptable balance between cpu and memory cost.
 *
 * @see: RLP classes: https:
 * TODO use http:
 */
public class IO {


    @Deprecated public static final byte SPECIAL_BYTE = (byte) 0xff;


    public static final int STREAM_BUFFER_SIZE = 64 * 1024;
    public static final int GZIP_BUFFER_SIZE =
            //512; //default
            1024;
    //4 * 1024;

    public static int readTasks(byte[] t, Consumer<Task> each) throws IOException {
        return readTasks(new ByteArrayInputStream(t), each);
    }


    public static int readTasks(InputStream i, Consumer<Task> each) throws IOException {

        DataInputStream ii = new DataInputStream(i);
        int count = 0;
        while (i.available() > 0 /*|| (i.available() > 0) || (ii.available() > 0)*/) {
            each.accept(TaskIO.readTask(ii));
            count++;
        }
        ii.close();
        return count;
    }


    /**
     * with Term first
     */
    private static void bytes(ByteArrayDataOutput out, Task t) throws IOException {


        byte p = t.punc();
        out.writeByte(p);


        TermIO.the.write(t.term(), out);


        if (p != COMMAND) {
            if (TaskIO.hasTruth(p))
                Truth.write(t.truth(), out);

            //TODO use delta zig zag encoding (with creation time too)
            out.writeLong(t.start());
            out.writeLong(t.end());

            writeEvidence(out, t.stamp());

            writeBudget(out, t);

            out.writeLong(t.creation());
        }

    }


    private static void writePriority(DataOutput out, Prioritized t) throws IOException {
        out.writeFloat(t.priElseZero());
    }

    private static void writeBudget(DataOutput out, Prioritized t) throws IOException {
        writePriority(out, t);
    }

    public static void writeEvidence(DataOutput out, long[] evi) throws IOException {
        int evil = evi.length;
        out.writeByte(evil);
        //TODO use zigzag delta encoding
        for (long anEvi : evi)
            out.writeLong(anEvi);
    }



    public static byte opAndSubType(Op op, byte subtype) {
        return opAndSubType(op.id, subtype);
    }

    private static byte opAndSubType(byte op, byte subtype) {
        return (byte) (op | (subtype << 5));
    }

    static byte subType(byte opByte) {
        return (byte) ((opByte & 0b11100000) >> 5);
    }


    public static byte[] termToBytes(Term t) {
        if (t instanceof Atomic) {
            return ((Atomic) t).bytes();
        } else {
            try (RecycledDynBytes d = RecycledDynBytes.get()) { //termBytesEstimate(t) /* estimate */);
                TermIO.the.write(t, d);
                return d.arrayCopy();
            }
        }
    }

//    /**
//     * warning: result may be RecycledDynBytes
//     */
//    public static DynBytes termToDynBytes(Term t) {
//        if (t instanceof Atomic) {
//            return new DynBytes(((Atomic) t).bytes()); //dont recycle
//        } else {
//            DynBytes d = new DynBytes(termBytesEstimate(t));
//            t.appendTo((ByteArrayDataOutput) d);
//            return d;
//        }
//    }

    public static int termBytesEstimate(Termlike t) {
        return t.volume() * 8;
    }


    @Nullable
    public static byte[] taskToBytes(Task x) {
        DynBytes dos = new DynBytes(termBytesEstimate(x));

        byte[] b = taskToBytes(x, dos);

        dos.close();

        return b;
    }

    public @Nullable
    static byte[] taskToBytes(Task x, DynBytes dos) {
        return bytes(x, dos).arrayCopy();
    }

    @Nullable
    public static DynBytes bytes(Task x, DynBytes dos) {
        try {


            dos.clear();
            IO.bytes(dos, x);


        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return dos;
    }

    /**
     * WARNING
     */
    @Nullable
    public static Term bytesToTerm(byte[] b) {
        try {
            return TermIO.the.read(TaskIO.input(b));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } /*catch (Term.InvalidTermException ignored) {
            return null;
        }*/
    }


//    @FunctionalInterface
//    public interface EachTerm {
//        void nextTerm(Op o, int depth, int byteStart);
//    }
//    public static void mapSubTerms(byte[] term, EachTerm t) throws IOException {
//
//        int l = term.length;
//        int i = 0;
//
//        int level = 0;
//        final int MAX_LEVELS = 16;
//        byte[][] levels = new byte[MAX_LEVELS][2];
//
//        do {
//
//            int termStart = i;
//            byte ob = term[i];
//            i++;
//            Op o = Op.values()[ob];
//            t.nextTerm(o, level, termStart);
//
//
//            if (o.var) {
//                i += 1;
//            } else if (o.atomic) {
//
//                int hi = term[i++] & 0xff;
//                int lo = term[i++] & 0xff;
//                int utfLen = (hi << 8) | lo;
//                i += utfLen;
//
//            } else {
//
//                int subterms = term[i++];
//                levels[level][0] = ob;
//                levels[level][1] = (byte) (subterms  /* include this? */);
//                level++;
//
//            }
//
//            //pop:
//            while (level > 0) {
//                byte[] ll = levels[level - 1];
//                byte subtermsRemain = ll[1];
//                if (subtermsRemain == 0) {
//
//                    Op ol = Op.values()[ll[0]];
//                    if (ol.temporal) {
//                        throw new TODO("i += IntCoding.variableByteLengthOfZigZagInt()");
//                        // 4;
//                    }
//                    level--;
//                } else {
//                    ll[1] = (byte) (subtermsRemain - 1);
//                    break;
//                }
//            }
//
//        } while (i < l);
//
//        if (i != l) {
//            throw new IOException("decoding error");
//        }
//    }


//    public static void writeUTF8WithPreLen(String s, DataOutput o) throws IOException {
//        DynBytes d = new DynBytes(s.length());
//
//        new Utf8Writer(d).write(s);
//
//        o.writeShort(d.length());
//        d.appendTo(o);
//    }

//    public enum TaskSerialization {
//        TermFirst,
//        TermLast
//    }


    /**
     * visits each subterm of a compound and stores a tuple of integers for it
     */



}





































































































