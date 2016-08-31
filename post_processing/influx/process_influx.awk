BEGIN {
    FS = ","
    OFS = "\t"
    print "subject","word","time","count" 
}
{ 
    if(($1) != "name") {
        split($2,subject,"=")
        split($3,word,"=")
        sub("\"","",word[2])
        print subject[2],word[2],$4,$5
        # print $1,$2,$3,$4,$5
    }
}
