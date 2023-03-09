art_url("http://localhost:8080/workspaces/online-disinfo/artifacts/img-annotator").
img_url("{imageUrl: https://i.imgur.com/5bGzZi7.jpg}").

!start.

+!start : true
    <- .print("I annotate web images.");
       .date(Y,M,D); .time(H,Min,Sec,MilSec);
       +started(Y,M,D,H,Min,Sec).

+!annotate_image_with_oldest_source: art_url(ArtUrl) & img_url(ImgUrl) <-
	.println("Executing the goal annotate images with oldest source");
    invokeAction(ArtUrl, "annotate image",[ImgUrl], Text)[artifact_id(AnnotatorId)];
    .print(Text).
