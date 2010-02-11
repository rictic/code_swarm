rm all.log
rm all.xml

cat *.log > all.log

python ~/src/codeswarm-read-only/convert_logs/convert_logs.py -s all.log -o all.xml
