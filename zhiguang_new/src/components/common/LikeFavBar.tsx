import { useEffect, useState } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import styles from "./LikeFavBar.module.css";
import { useAuth } from "../../context/AuthContext";
import { BookmarkIcon, HeartIcon } from "../icons/Icon";

type LikeFavBarProps = {
  entityId: string;
  entityType?: string; // default: "knowpost"
  initialCounts?: { like: number; fav: number };
  initialState?: { liked?: boolean; faved?: boolean };
  fetchCounts?: boolean; // if true, fetch counts on mount (requires auth per current policy)
  compact?: boolean;
  className?: string;
};

const LikeFavBar = ({
  entityId,
  entityType = "knowpost",
  initialCounts,
  initialState,
  fetchCounts = false,
  compact = false,
  className,
}: LikeFavBarProps) => {
  const { tokens } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const iconSize = compact ? 18 : 20;

  const [likeCount, setLikeCount] = useState<number>(initialCounts?.like ?? 0);
  const [favCount, setFavCount] = useState<number>(initialCounts?.fav ?? 0);
  const [liked, setLiked] = useState<boolean>(initialState?.liked ?? false);
  const [faved, setFaved] = useState<boolean>(initialState?.faved ?? false);
  const [loadingLike, setLoadingLike] = useState(false);
  const [loadingFav, setLoadingFav] = useState(false);
  //todo
  return (
    <>
      <div> 未完工</div>
    </>
  );
};

export default LikeFavBar;
