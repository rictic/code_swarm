
SVN="https://subversion.ops.ag.com/repos/"

svn log -v -r48016:HEAD ${SVN}pw/trunk/quadshot > quadshot.log
svn log -v ${SVN}agcom/java-projects/text_renderer/trunk > rendertext.log
svn log -v ${SVN}agcom/java-projects/rendering/trunk > rendering.log
svn log -v ${SVN}agcom/java-projects/renderWeb/trunk > renderweb.log
svn log -v ${SVN}agcom/websites/img/trunk/html/pw > images.log


# TODO: regenerate all other logs
# TODO: trim last SVN separator from log
# TODO: concatenate all svn logs
