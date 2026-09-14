package vn.edu.donga.unischedule.util;
import javax.imageio.ImageIO;
import java.io.*;
import java.nio.file.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import vn.edu.donga.unischedule.validation.ValidationException;
public final class ProfileImages {
    public static final int MAX_BYTES=5*1024*1024;
    private ProfileImages() { }
    public static byte[] read(Path path) {
        try {
            if(Files.size(path)>MAX_BYTES) throw new ValidationException("Ảnh không được vượt quá 5 MB.");
            byte[] bytes;
            try(var file=Files.newInputStream(path)) { bytes=file.readNBytes(MAX_BYTES+1); }
            if(bytes.length>MAX_BYTES) throw new ValidationException("Ảnh không được vượt quá 5 MB.");
            try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers=ImageIO.getImageReaders(input);
                if(!readers.hasNext()) throw new ValidationException("Chọn ảnh PNG hoặc JPEG hợp lệ.");
                var reader=readers.next();
                try {
                    String format=reader.getFormatName();
                    if(!format.equalsIgnoreCase("png") && !format.equalsIgnoreCase("jpeg")) throw new ValidationException("Chỉ hỗ trợ PNG và JPEG.");
                    reader.setInput(input,true,true);
                    int width=reader.getWidth(0),height=reader.getHeight(0);
                    if(width<=0 || height<=0 || (long)width*height>20000000) throw new ValidationException("Ảnh tối đa 20 triệu điểm ảnh.");
                    var source=reader.read(0);var result=new BufferedImage(512,512,BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g=result.createGraphics();
                    try { g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);int edge=Math.min(width,height),x=(width-edge)/2,y=(height-edge)/2;g.drawImage(source,0,0,512,512,x,y,x+edge,y+edge,null); }
                    finally { g.dispose(); }
                    var output=new ByteArrayOutputStream();ImageIO.write(result,"png",output);return output.toByteArray();
                } finally { reader.dispose(); }
            }
        } catch(IOException ex) { throw new ValidationException("Không đọc được ảnh. Hãy chọn lại tệp PNG hoặc JPEG."); }
    }
}
